/*    
 *    MediathekView
 *    Copyright (C) 2008   W. Xaver
 *    W.Xaver[at]googlemail.com
 *    http://zdfmediathk.sourceforge.net/
 *    
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package mediathek.controller.starter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import mediathek.daten.Daten;
import mediathek.daten.DatenDownload;
import mediathek.daten.DatenPset;
import mediathek.tool.Datum;
import mediathek.tool.Konstanten;
import mediathek.tool.ListenerMediathekView;
import mediathek.tool.Log;
import mediathek.tool.MVInputStream;
import mediathek.tool.MVNotification;
import mediathek.tool.MVUrlDateiGroesse;
import msearch.daten.DatenFilm;

public class StarterClass {
    //Tags Filme

    private Daten daten;
    private Starten starten = null;
    private boolean pause = false;

    //===================================
    // Public
    //===================================
    public StarterClass(Daten d) {
        daten = d;
        starten = new Starten();
        Thread startenThread = new Thread(starten);
        startenThread.setDaemon(true);
        startenThread.start();
    }

    public synchronized Start urlStarten(DatenPset pSet, DatenFilm ersterFilm) {
        // url mit dem Programm mit der Nr. starten (Button oder Doppelklick)
        // Quelle "Button" ist immer ein vom User gestarteter Film, also Quelle_Button!!!!!!!!!!!
        Start s = null;
        String url = ersterFilm.arr[DatenFilm.FILM_URL_NR];
        if (!url.equals("")) {
            DatenDownload d = new DatenDownload(pSet, ersterFilm, Start.QUELLE_BUTTON, null, "", "", "" /*Aufloesung*/);
            d.start = new Start();
            //gestartete Filme (originalURL des Films) auch in die History eintragen
            daten.history.add(d.arr[DatenDownload.DOWNLOAD_FILM_URL_NR]);
            starten.startStarten(d);
        }
        return s;
    }

    public void pause() {
        pause = true;
    }

    // ********************************************
    // Hier wird dann gestartet
    // Ewige Schleife die die Downloads startet
    // ********************************************
    private class Starten implements Runnable {

        DatenDownload datenDownload;

        @Override
        public synchronized void run() {
            while (true) {
                try {
                    while ((datenDownload = getNextStart()) != null) {
                        startStarten(datenDownload);
                        //alle 5 Sekunden einen Download starten
                        this.wait(5 * 1000);
                    }
//                    Daten.listeDownloads.buttonStartsPutzen(); // Button Starts aus der Liste löschen
                    this.wait(3 * 1000);
                } catch (Exception ex) {
                    Log.fehlerMeldung(613822015, Log.FEHLER_ART_PROG, "StarterClass.Starten.run", ex);
                }
            } //while(true)
        }

        private synchronized DatenDownload getNextStart() throws InterruptedException {
            // get: erstes passendes Element der Liste zurückgeben oder null
            // und versuchen dass bei mehreren laufenden Downloads ein anderer Sender gesucht wird
            if (pause) {
                // beim Löschen der Downloads, kann das Starten etwas "pausiert" werden
                // damit ein zu Löschender Download nicht noch schnell gestartet wird
                this.wait(5 * 1000);
                pause = false;
            }
            return Daten.listeDownloads.getNextStart();
        }

        private void startStarten(DatenDownload datenDownload) {
            datenDownload.start.startZeit = new Datum();
            ListenerMediathekView.notify(ListenerMediathekView.EREIGNIS_ART_DOWNLOAD_PROZENT, StarterClass.class.getName());
            switch (datenDownload.getArt()) {
                case Start.ART_PROGRAMM:
                    StartenProgramm startenProgrammn = new StartenProgramm(datenDownload);
                    new Thread(startenProgrammn).start();
                    break;
                case Start.ART_DOWNLOAD:
                    StartenDownload startenDownload = new StartenDownload(datenDownload);
                    new Thread(startenDownload).start();
                    break;
                default:
                    Log.fehlerMeldung(789356001, Log.FEHLER_ART_PROG, "StartetClass.startStarten", "StarterClass.Starten - Switch-default");
                    break;
            }
        }
    }

    private class StartenProgramm implements Runnable {

        DatenDownload datenDownload;
        RuntimeExec runtimeExec;
        File file;

        public StartenProgramm(DatenDownload d) {
            datenDownload = d;
            datenDownload.start.status = Start.STATUS_RUN;
            file = new File(datenDownload.arr[DatenDownload.DOWNLOAD_ZIEL_PFAD_DATEINAME_NR]);
            notifyStartEvent();
            try {
                new File(datenDownload.arr[DatenDownload.DOWNLOAD_ZIEL_PFAD_NR]).mkdirs();
            } catch (Exception ex) {
                Log.fehlerMeldung(469365281, Log.FEHLER_ART_PROG, "StarterClass.StartenProgramm-1", ex);
            }
        }

        @Override
        public synchronized void run() {
            long filesize = -1;
            final int stat_start = 0;
            final int stat_laufen = 1;
            final int stat_restart = 3;
            final int stat_pruefen = 4;
            // ab hier ist schluss
            final int stat_fertig_ok = 10;
            final int stat_fertig_fehler = 11;
            final int stat_ende = 99;
            int stat = stat_start;
            try {
                while (stat < stat_ende) {
                    switch (stat) {
                        case stat_start:
                            // versuch das Programm zu Starten
                            if (starten()) {
                                stat = stat_laufen;
                            } else {
                                stat = stat_restart;
                            }
                            break;
                        case stat_laufen:
                            //hier läuft der Download bis zum Abbruch oder Ende
                            try {
                                if (datenDownload.start.stoppen) {
                                    stat = stat_fertig_ok;
                                    if (datenDownload.start.process != null) {
                                        datenDownload.start.process.destroy();
                                    }
                                } else {
                                    int exitV = datenDownload.start.process.exitValue();
                                    if (exitV != 0) {
                                        stat = stat_restart;
                                    } else {
                                        stat = stat_pruefen;
                                    }
                                }
                            } catch (Exception ex) {
                                try {
                                    this.wait(2000);
                                } catch (InterruptedException e) {
                                }
                            }
                            break;
                        case stat_restart:
                            if (!datenDownload.isRestart()) {
                                // dann wars das
                                stat = stat_fertig_fehler;
                            } else {
                                if (filesize == -1) {
                                    //noch nichts geladen
                                    leeresFileLoeschen(file);
                                    if (file.exists()) {
                                        // dann bestehende Datei weitermachen
                                        filesize = file.length();
                                        stat = stat_start;
                                    } else {
                                        // counter prüfen und bei einem Maxwert abbrechen, sonst endlos
                                        if (datenDownload.start.startcounter < Start.STARTCOUNTER_MAX) {
                                            // dann nochmal von vorne
                                            stat = stat_start;
                                        } else {
                                            // dann wars das
                                            stat = stat_fertig_fehler;
                                        }
                                    }
                                } else {
                                    //jetzt muss das File wachsen, sonst kein Restart
                                    if (!file.exists()) {
                                        // dann wars das
                                        stat = stat_fertig_fehler;
                                    } else {
                                        if (file.length() > filesize) {
                                            //nur weitermachen wenn die Datei tasächlich wächst
                                            filesize = file.length();
                                            stat = stat_start;
                                        } else {
                                            // dann wars das
                                            stat = stat_fertig_fehler;
                                        }
                                    }
                                }
                            }
                            break;
                        case stat_pruefen:
                            if (datenDownload.getQuelle() == Start.QUELLE_BUTTON) {
                                //für die direkten Starts mit dem Button wars das dann
                                stat = stat_fertig_ok;
                            } else if (pruefen(datenDownload)) {
                                //fertig und OK
                                stat = stat_fertig_ok;
                            } else {
                                //fertig und fehlerhaft
                                stat = stat_fertig_fehler;
                            }
                            break;
                        case stat_fertig_fehler:
                            datenDownload.start.status = Start.STATUS_ERR;
                            stat = stat_ende;
                            break;
                        case stat_fertig_ok:
                            datenDownload.start.status = Start.STATUS_FERTIG;
                            stat = stat_ende;
                            break;
                    }
                }
            } catch (Exception ex) {
                Log.fehlerMeldung(395623710, Log.FEHLER_ART_PROG, "StarterClass.StartenProgramm-2", ex);
            }
            leeresFileLoeschen(file);
            fertigmeldung(datenDownload);
            datenDownload.start.restSekunden = -1;
            datenDownload.start.percent = Start.PROGRESS_FERTIG;
            ListenerMediathekView.notify(ListenerMediathekView.EREIGNIS_ART_DOWNLOAD_PROZENT, StarterClass.class.getName());
//            start.datenDownload.statusMelden(DatenDownload.PROGRESS_FERTIG);
            notifyStartEvent();
        }

        private boolean starten() {
            boolean ret = false;
            // die Reihenfolge: startcounter - startmeldung ist wichtig!
            datenDownload.start.startcounter++;
            startmeldung(datenDownload);
            runtimeExec = new RuntimeExec(datenDownload);
            datenDownload.start.process = runtimeExec.exec();
            if (datenDownload.start.process != null) {
                ret = true;
            }
            return ret;
        }
    }

    private class StartenDownload implements Runnable {

        DatenDownload datenDownload;

        public StartenDownload(DatenDownload d) {
            datenDownload = d;
            datenDownload.start.status = Start.STATUS_RUN;
            notifyStartEvent();
        }

        @Override
        public void run() {
            startmeldung(datenDownload);
            MVInputStream input;
            OutputStream destStream;
            try {
                int len;
                new File(datenDownload.arr[DatenDownload.DOWNLOAD_ZIEL_PFAD_NR]).mkdirs();
                datenDownload.mVFilmSize.setSize(MVUrlDateiGroesse.laenge(datenDownload.arr[DatenDownload.DOWNLOAD_URL_NR]));
                datenDownload.mVFilmSize.setAktSize(0);
                BufferedInputStream srcBuffer = null;
                BufferedOutputStream destBuffer = null;
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(datenDownload.arr[DatenDownload.DOWNLOAD_URL_NR]);
                    conn = (HttpURLConnection) url.openConnection();
                    File file = new File(datenDownload.arr[DatenDownload.DOWNLOAD_ZIEL_PFAD_DATEINAME_NR]);
//                    int downloaded = 0;
//                    if (file.exists()) {
//                        downloaded = (int) file.length();
//                        conn.setRequestProperty("Range", "bytes=" + downloaded + "-");
//                        conn.connect();
//                    } else {
                    conn.setRequestProperty("User-Agent", Daten.getUserAgent());
                    conn.connect();
//                    }
//                    RandomAccessFile outFile = new RandomAccessFile(start.datenDownload.arr[DatenDownload.DOWNLOAD_ZIEL_PFAD_DATEINAME_NR], "rw");
//                    if (downloaded > 0) {
//                        outFile.seek(downloaded);
//                    }
                    if (conn.getResponseCode() >= 400) {
                        Log.fehlerMeldung(915236798, Log.FEHLER_ART_PROG, "StartetClass", "HTTP-Fehler: " + conn.getResponseCode() + " " + conn.getResponseMessage());
                    }
                    input = new MVInputStream(conn.getInputStream());
                    datenDownload.start.mVInputStream = input;
                    srcBuffer = new BufferedInputStream(input);
//                    FileOutputStream fos = (downloaded == 0) ? new FileOutputStream(file) : new FileOutputStream(file, true);
//                    destBuffer = new BufferedOutputStream(fos, 1024);
                    destBuffer = new BufferedOutputStream(new FileOutputStream(file), 1024);
                    byte[] buffer = new byte[1024];
                    long p, pp = 0;
                    while ((len = srcBuffer.read(buffer)) != -1 && !datenDownload.start.stoppen) {
//                        outFile.write(buffer, downloaded, len);
//                        downloaded += len;
                        destBuffer.write(buffer, 0, len);
                        datenDownload.mVFilmSize.addAktSize(len);
                        if (datenDownload.mVFilmSize.getSize() > 0) {
                            p = (datenDownload.mVFilmSize.getAktSize() * (long) 1000) / datenDownload.mVFilmSize.getSize();
                            // p muss zwischen 1 und 999 liegen
                            if (p == 0) {
                                p = Start.PROGRESS_GESTARTET;
                            } else if (p >= 1000) {
                                p = 999;
                            }
                            datenDownload.start.percent = (int) p;
                            if (p != pp) {
                                pp = p;
                                // Restzeit ermitteln
                                if (p > 2) {
                                    // sonst macht es noch keinen Sinn
                                    datenDownload.start.bandbreite = input.getBandbreite();
                                    int diffZeit = datenDownload.start.startZeit.diffInSekunden();
                                    int restProzent = 1000 - (int) p;
                                    datenDownload.start.restSekunden = (diffZeit * restProzent / p);
                                }
                                ListenerMediathekView.notify(ListenerMediathekView.EREIGNIS_ART_DOWNLOAD_PROZENT, StarterClass.class.getName());
                            }
                        }
                    }
                    Log.systemMeldung(input.toString());
                } catch (Exception ex) {
                    Log.fehlerMeldung(316598941, Log.FEHLER_ART_PROG, "StartetClass.leeresFileLoeschen", ex, "Fehler");
                } finally {
                    try {
                        if (srcBuffer != null) {
                            srcBuffer.close();
                        }
                        if (destBuffer != null) {
                            destBuffer.close();
                        }
                        if (conn != null) {
                            conn.disconnect();
                        }
                    } catch (Exception e) {
                    }
                }
            } catch (Exception ex) {
                Log.fehlerMeldung(502039078, Log.FEHLER_ART_PROG, "StarterClass.StartenDownload-1", ex);
            }
            if (!datenDownload.start.stoppen) {
                if (datenDownload.getQuelle() == Start.QUELLE_BUTTON) {
                    // direkter Start mit dem Button
                    datenDownload.start.status = Start.STATUS_FERTIG;
                } else if (pruefen(datenDownload)) {
                    //Anzeige ändern - fertig
                    datenDownload.start.status = Start.STATUS_FERTIG;
                } else {
                    //Anzeige ändern - bei Fehler fehlt der Eintrag
                    datenDownload.start.status = Start.STATUS_ERR;
                }
            }
            leeresFileLoeschen(new File(datenDownload.arr[DatenDownload.DOWNLOAD_ZIEL_PFAD_DATEINAME_NR]));
            fertigmeldung(datenDownload);
            datenDownload.start.restSekunden = -1;
            datenDownload.start.percent = Start.PROGRESS_FERTIG;
            datenDownload.mVFilmSize.setAktSize(-1);
            ListenerMediathekView.notify(ListenerMediathekView.EREIGNIS_ART_DOWNLOAD_PROZENT, StarterClass.class.getName());
            notifyStartEvent();
        }
    }

//    private void programmBeenden() {
//        if (ddaten.nachDownloadShutDown) {
//            // Sicherheitsabfrage, dann beenden
//        }
//    }
    private boolean pruefen(DatenDownload datenDownload) {
        //prüfen ob der Downoad geklappt hat und die Datei existiert und eine min. Grüße hat
        boolean ret = false;
        File file = new File(datenDownload.arr[DatenDownload.DOWNLOAD_ZIEL_PFAD_DATEINAME_NR]);
        if (!file.exists()) {
            Log.fehlerMeldung(550236231, Log.FEHLER_ART_PROG, "StartetClass.pruefen-1", "Download fehlgeschlagen: Datei existiert nicht" + datenDownload.arr[DatenDownload.DOWNLOAD_ZIEL_PFAD_DATEINAME_NR]);
        } else if (file.length() < Konstanten.MIN_DATEI_GROESSE_FILM) {
            Log.fehlerMeldung(795632500, Log.FEHLER_ART_PROG, "StartetClass.pruefen-2", "Download fehlgeschlagen: Datei zu klein" + datenDownload.arr[DatenDownload.DOWNLOAD_ZIEL_PFAD_DATEINAME_NR]);
        } else if (datenDownload.start.percent > -1 && datenDownload.start.percent < 995) {
            // Prozent werden berechnet und es wurde vor 99,5% abgebrochen
            Log.fehlerMeldung(696510258, Log.FEHLER_ART_PROG, "StartetClass.pruefen-3", "Download fehlgeschlagen: 99,5% wurden nicht erreicht" + datenDownload.arr[DatenDownload.DOWNLOAD_ZIEL_PFAD_DATEINAME_NR]);
        } else {
            if (datenDownload.istAbo()) {
                daten.erledigteAbos.zeileSchreiben(datenDownload.arr[DatenDownload.DOWNLOAD_THEMA_NR],
                        datenDownload.arr[DatenDownload.DOWNLOAD_TITEL_NR],
                        datenDownload.arr[DatenDownload.DOWNLOAD_FILM_URL_NR]);
            }
            ret = true;
        }
        return ret;
    }

    private void leeresFileLoeschen(File file) {
        //prüfen ob die Datei existiert und eine min. Grüße hat, wenn nicht, dann löschen
        try {
            if (file.exists()) {
                // zum Wiederstarten/Aufräumen die leer/zu kleine Datei löschen, alles auf Anfang
                if (file.length() == 0) {
                    // zum Wiederstarten/Aufräumen die leer/zu kleine Datei löschen, alles auf Anfang
                    Log.systemMeldung(new String[]{"Restart/Aufräumen: leere Datei löschen", file.getAbsolutePath()});
                    file.delete();
                } else if (file.length() < Konstanten.MIN_DATEI_GROESSE_FILM) {
                    Log.systemMeldung(new String[]{"Restart/Aufräumen: Zu kleine Datei löschen", file.getAbsolutePath()});
                    file.delete();
                }
            }
        } catch (Exception ex) {
            Log.fehlerMeldung(795632500, Log.FEHLER_ART_PROG, "StartetClass.leeresFileLoeschen", "Fehler beim löschen" + file.getAbsolutePath());
        }
    }

    private void startmeldung(DatenDownload datenDownload) {
        ArrayList<String> text = new ArrayList<String>();
        boolean abspielen = datenDownload.getQuelle() == Start.QUELLE_BUTTON;
        if (abspielen) {
            text.add("Film starten");
        } else {
            if (datenDownload.start.startcounter > 1) {
                text.add("Download starten - Restart (Summe Starts: " + datenDownload.start.startcounter + ")");
            } else {
                text.add("Download starten");
            }
            text.add("Programmset: " + datenDownload.arr[DatenDownload.DOWNLOAD_PROGRAMMSET_NR]);
            text.add("Ziel: " + datenDownload.arr[DatenDownload.DOWNLOAD_ZIEL_PFAD_DATEINAME_NR]);
        }
        text.add("URL: " + datenDownload.arr[DatenDownload.DOWNLOAD_URL_NR]);
        text.add("Startzeit: " + new SimpleDateFormat("HH:mm:ss").format(datenDownload.start.startZeit));
        if (datenDownload.getArt() == Start.ART_DOWNLOAD) {
            text.add(Start.ART_DOWNLOAD_TXT);
        } else {
            text.add("Programmaufruf: " + datenDownload.arr[DatenDownload.DOWNLOAD_PROGRAMM_AUFRUF_NR]);
        }
        Log.systemMeldung(text.toArray(new String[]{}));
    }

    private void fertigmeldung(DatenDownload datenDownload) {
        ArrayList<String> text = new ArrayList<String>();
        boolean abspielen = datenDownload.getQuelle() == Start.QUELLE_BUTTON;
        if (abspielen) {
            text.add("Film fertig");
        } else {
            if (datenDownload.start.stoppen) {
                text.add("Download abgebrochen");
            } else if (datenDownload.start.status != Start.STATUS_ERR) {
                // dann ists gut
                text.add("Download ist fertig und hat geklappt");
            } else {
                text.add("Download ist fertig und war fehlerhaft");
            }
            text.add("Programmset: " + datenDownload.arr[DatenDownload.DOWNLOAD_PROGRAMMSET_NR]);
            text.add("Ziel: " + datenDownload.arr[DatenDownload.DOWNLOAD_ZIEL_PFAD_DATEINAME_NR]);
        }
        text.add("Startzeit: " + new SimpleDateFormat("HH:mm:ss").format(datenDownload.start.startZeit));
        long dauer = datenDownload.start.startZeit.diffInMinuten();
        if (dauer == 0) {
            text.add("Dauer: <1 Min.");
        } else {
            text.add("Dauer: " + datenDownload.start.startZeit.diffInMinuten() + " Min");
        }
        text.add("URL: " + datenDownload.arr[DatenDownload.DOWNLOAD_URL_NR]);
        if (datenDownload.getArt() == Start.ART_DOWNLOAD) {
            text.add(Start.ART_DOWNLOAD_TXT);
        } else {
            text.add("Programmaufruf: " + datenDownload.arr[DatenDownload.DOWNLOAD_PROGRAMM_AUFRUF_NR]);
        }
        Log.systemMeldung(text.toArray(new String[]{}));
        if (!datenDownload.start.stoppen) {
            MVNotification.addNotification(datenDownload, datenDownload.start.status != Start.STATUS_ERR);
        }
    }

    private void notifyStartEvent() {
        ListenerMediathekView.notify(ListenerMediathekView.EREIGNIS_START_EVENT, StarterClass.class.getSimpleName());
    }
}