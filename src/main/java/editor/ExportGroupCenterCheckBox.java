package editor;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

final class ExportGroupCenterCheckBox extends JCheckBox {
    enum CenterState {
        EMPTY,
        CURRENT_MAP,
        OTHER_MAP
    }

    private final Icon emptyIcon = new CenterIcon(false, false, false);
    private final Icon currentMapIcon = new CenterIcon(true, true, false);
    private final Icon otherMapIcon = new CenterIcon(true, false, false);
    private final Icon disabledEmptyIcon = new CenterIcon(false, false, true);
    private final Icon disabledCurrentMapIcon = new CenterIcon(true, true, true);
    private final Icon disabledOtherMapIcon = new CenterIcon(true, false, true);

    private CenterState centerState = CenterState.EMPTY;
    private boolean applyingCenterState;

    ExportGroupCenterCheckBox() {
        setCenterState(CenterState.EMPTY);
        setEnabled(false);
    }

    void setCenterState(CenterState centerState) {
        applyingCenterState = true;
        try {
            this.centerState = centerState;
            setSelected(centerState == CenterState.CURRENT_MAP);
            setIcon(centerState == CenterState.OTHER_MAP ? otherMapIcon : emptyIcon);
            setSelectedIcon(currentMapIcon);
            setDisabledIcon(centerState == CenterState.OTHER_MAP ? disabledOtherMapIcon : disabledEmptyIcon);
            setDisabledSelectedIcon(disabledCurrentMapIcon);
            setToolTipText(getToolTipFor(centerState));
        } finally {
            applyingCenterState = false;
        }
        repaint();
    }

    CenterState getCenterState() {
        return centerState;
    }

    boolean isApplyingCenterState() {
        return applyingCenterState;
    }

    private static String getToolTipFor(CenterState centerState) {
        switch (centerState) {
            case CURRENT_MAP:
                return "This map is the export group center.";
            case OTHER_MAP:
                return "Another map is the export group center.";
            default:
                return "This export group has no center.";
        }
    }

    private static final class CenterIcon implements Icon {
        private static final int SIZE = 13;
        private final boolean filled;
        private final boolean checked;
        private final boolean disabled;

        private CenterIcon(boolean filled, boolean checked, boolean disabled) {
            this.filled = filled;
            this.checked = checked;
            this.disabled = disabled;
        }

        @Override
        public int getIconWidth() {
            return SIZE;
        }

        @Override
        public int getIconHeight() {
            return SIZE;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color borderColor = disabled ? new Color(130, 130, 130) : new Color(95, 95, 95);
            Color fillColor = disabled ? new Color(150, 150, 150) : new Color(60, 130, 210);
            Color checkColor = disabled ? new Color(235, 235, 235) : Color.WHITE;

            if (filled) {
                g2d.setColor(fillColor);
                g2d.fillRect(x + 2, y + 2, SIZE - 4, SIZE - 4);
            }

            g2d.setColor(borderColor);
            g2d.drawRect(x + 1, y + 1, SIZE - 3, SIZE - 3);

            if (checked) {
                g2d.setColor(checkColor);
                g2d.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2d.drawLine(x + 4, y + 7, x + 6, y + 9);
                g2d.drawLine(x + 6, y + 9, x + 10, y + 4);
            }

            g2d.dispose();
        }
    }
}
