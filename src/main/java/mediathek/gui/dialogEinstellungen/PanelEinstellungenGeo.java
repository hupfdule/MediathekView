package mediathek.gui.dialogEinstellungen;

import mediathek.config.Daten;
import mediathek.config.Icons;
import mediathek.daten.GeoblockingField;
import mediathek.file.GetFile;
import mediathek.gui.dialog.DialogHilfe;
import mediathek.gui.messages.GeoStateChangedEvent;
import mediathek.tool.ApplicationConfiguration;
import mediathek.tool.Listener;
import org.apache.commons.configuration2.Configuration;

import javax.swing.*;
import javax.swing.border.TitledBorder;

@SuppressWarnings("serial")
public class PanelEinstellungenGeo extends JPanel {
    private final JFrame parentComponent;

    public PanelEinstellungenGeo(JFrame pparentComponent) {
        parentComponent = pparentComponent;

        initComponents();
        init();
    }

    private void init() {
        final Configuration config = ApplicationConfiguration.getConfiguration();

        switch (config.getString(ApplicationConfiguration.GEO_LOCATION)) {
            case GeoblockingField.GEO_CH:
                jRadioButtonCH.setSelected(true);
                break;
            case GeoblockingField.GEO_AT:
                jRadioButtonAt.setSelected(true);
                break;
            case GeoblockingField.GEO_EU:
                jRadioButtonEu.setSelected(true);
                break;
            case GeoblockingField.GEO_WELT:
                jRadioButtonSonst.setSelected(true);
                break;
            default:
                jRadioButtonDe.setSelected(true);
        }
        jRadioButtonDe.addActionListener(e -> {
            config.setProperty(ApplicationConfiguration.GEO_LOCATION, GeoblockingField.GEO_DE);
            melden();
        });
        jRadioButtonCH.addActionListener(e -> {
            config.setProperty(ApplicationConfiguration.GEO_LOCATION, GeoblockingField.GEO_CH);
            melden();
        });
        jRadioButtonAt.addActionListener(e -> {
            config.setProperty(ApplicationConfiguration.GEO_LOCATION, GeoblockingField.GEO_AT);
            melden();
        });
        jRadioButtonEu.addActionListener(e -> {
            config.setProperty(ApplicationConfiguration.GEO_LOCATION, GeoblockingField.GEO_EU);
            melden();
        });
        jRadioButtonSonst.addActionListener(e -> {
            config.setProperty(ApplicationConfiguration.GEO_LOCATION, GeoblockingField.GEO_WELT);
            melden();
        });

        jCheckBoxMarkieren.setSelected(config.getBoolean(ApplicationConfiguration.GEO_REPORT));
        jCheckBoxMarkieren.addActionListener(e -> {
            config.setProperty(ApplicationConfiguration.GEO_REPORT, jCheckBoxMarkieren.isSelected());
            melden();
        });
        jButtonHilfe.setIcon(Icons.ICON_BUTTON_HELP);
        jButtonHilfe.addActionListener(e -> new DialogHilfe(parentComponent, true, new GetFile().getHilfeSuchen(GetFile.PFAD_HILFETEXT_GEO)).setVisible(true));
    }

    private void melden() {
        var daten = Daten.getInstance();
        daten.getListeBlacklist().filterListe();
        daten.getMessageBus().publishAsync(new GeoStateChangedEvent());
        Listener.notify(Listener.EREIGNIS_BLACKLIST_GEAENDERT, PanelEinstellungenGeo.class.getSimpleName());

    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    // Generated using JFormDesigner non-commercial license
    private void initComponents() {
        var jPanel6 = new JPanel();
        jCheckBoxMarkieren = new JCheckBox();
        jRadioButtonDe = new JRadioButton();
        var jLabel1 = new JLabel();
        jRadioButtonCH = new JRadioButton();
        jRadioButtonAt = new JRadioButton();
        jRadioButtonEu = new JRadioButton();
        jRadioButtonSonst = new JRadioButton();
        jButtonHilfe = new JButton();

        //======== this ========

        //======== jPanel6 ========
        {
            jPanel6.setBorder(new TitledBorder("Geogeblockte Filme")); //NON-NLS

            //---- jCheckBoxMarkieren ----
            jCheckBoxMarkieren.setText("geblockte Sendungen gelb markieren"); //NON-NLS

            //---- jRadioButtonDe ----
            jRadioButtonDe.setSelected(true);
            jRadioButtonDe.setText("DE - Deutschland"); //NON-NLS

            //---- jLabel1 ----
            jLabel1.setText("Mein Standort:"); //NON-NLS

            //---- jRadioButtonCH ----
            jRadioButtonCH.setText("CH - Schweiz"); //NON-NLS

            //---- jRadioButtonAt ----
            jRadioButtonAt.setText("AT - \u00d6sterreich"); //NON-NLS

            //---- jRadioButtonEu ----
            jRadioButtonEu.setText("EU (EBU - European Broadcasting Union)"); //NON-NLS

            //---- jRadioButtonSonst ----
            jRadioButtonSonst.setText("sonst"); //NON-NLS

            //---- jButtonHilfe ----
            jButtonHilfe.setIcon(new ImageIcon(getClass().getResource("/mediathek/res/muster/button-help.png"))); //NON-NLS
            jButtonHilfe.setToolTipText("Hilfe anzeigen"); //NON-NLS

            GroupLayout jPanel6Layout = new GroupLayout(jPanel6);
            jPanel6.setLayout(jPanel6Layout);
            jPanel6Layout.setHorizontalGroup(
                jPanel6Layout.createParallelGroup()
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel6Layout.createParallelGroup()
                            .addComponent(jRadioButtonSonst)
                            .addComponent(jRadioButtonEu)
                            .addComponent(jCheckBoxMarkieren)
                            .addComponent(jLabel1)
                            .addComponent(jRadioButtonDe)
                            .addComponent(jRadioButtonCH)
                            .addComponent(jRadioButtonAt))
                        .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                        .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButtonHilfe)
                        .addContainerGap())
            );
            jPanel6Layout.setVerticalGroup(
                jPanel6Layout.createParallelGroup()
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jCheckBoxMarkieren)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel1)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jRadioButtonDe)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jRadioButtonCH)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jRadioButtonAt)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jRadioButtonEu)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jRadioButtonSonst)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 38, Short.MAX_VALUE)
                        .addComponent(jButtonHilfe)
                        .addContainerGap())
            );
        }

        GroupLayout layout = new GroupLayout(this);
        setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup()
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jPanel6, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup()
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jPanel6, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        //---- buttonGroup1 ----
        var buttonGroup1 = new ButtonGroup();
        buttonGroup1.add(jRadioButtonDe);
        buttonGroup1.add(jRadioButtonCH);
        buttonGroup1.add(jRadioButtonAt);
        buttonGroup1.add(jRadioButtonEu);
        buttonGroup1.add(jRadioButtonSonst);
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // Generated using JFormDesigner non-commercial license
    private JCheckBox jCheckBoxMarkieren;
    private JRadioButton jRadioButtonDe;
    private JRadioButton jRadioButtonCH;
    private JRadioButton jRadioButtonAt;
    private JRadioButton jRadioButtonEu;
    private JRadioButton jRadioButtonSonst;
    private JButton jButtonHilfe;
    // End of variables declaration//GEN-END:variables

}
