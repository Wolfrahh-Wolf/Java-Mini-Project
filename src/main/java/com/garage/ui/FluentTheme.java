/**
 * FluentTheme — Microsoft Fluent UI 2 Dark Mode design tokens and shared UI helpers.
 * All color constants, typography settings, and factory methods live here.
 */
package com.garage.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;

/**
 * Static registry of Fluent UI 2 Dark Mode tokens and shared Swing factory utilities.
 *
 * <p>Color names map 1-to-1 with the official Fluent 2 design vocabulary:</p>
 * <ul>
 *   <li>{@link #CANVAS}       — primary page background (#1E1E1E)</li>
 *   <li>{@link #SURFACE}      — card / panel fill (#292929)</li>
 *   <li>{@link #BORDER}       — divider / outline (#3D3D3D)</li>
 *   <li>{@link #INPUT_BG}     — text-field / combo background (#333333)</li>
 *   <li>{@link #ACCENT}       — Fluent Blue primary (#0D7CF2)</li>
 *   <li>{@link #ACCENT_HOVER} — Fluent Blue hover (#0078D4)</li>
 *   <li>{@link #TEXT_PRIMARY} — off-white primary text (#FFFFFF)</li>
 *   <li>{@link #TEXT_MUTED}   — secondary / caption text (#ADADAD)</li>
 *   <li>{@link #SELECTION_BG} — row-selection fill (#1C3B5E)</li>
 *   <li>{@link #GRID_LINE}    — JTable horizontal grid (#333333)</li>
 *   <li>{@link #HEADER_BG}    — table header background (#242424)</li>
 * </ul>
 */
public final class FluentTheme {

    // ── Color Tokens ──────────────────────────────────────────────────────────
    /** Canvas background — outermost page/panel fill. */
    public static final Color CANVAS       = new Color(0x1E1E1E);
    /** Surface — card-level fill, one level above canvas. */
    public static final Color SURFACE      = new Color(0x292929);
    /** Slightly elevated surface, e.g. toolbar / form section. */
    public static final Color SURFACE_ALT  = new Color(0x2E2E2E);
    /** Border and divider color. */
    public static final Color BORDER       = new Color(0x3D3D3D);
    /** Input field background. */
    public static final Color INPUT_BG     = new Color(0x333333);
    /** Fluent Blue — primary accent. */
    public static final Color ACCENT       = new Color(0x0D7CF2);
    /** Fluent Blue — hovered / pressed state. */
    public static final Color ACCENT_HOVER = new Color(0x0078D4);
    /** Accent with low opacity — used for focus rings, selection states. */
    public static final Color ACCENT_MUTED = new Color(0x1C3B5E);
    /** Primary (off-white) text. */
    public static final Color TEXT_PRIMARY = new Color(0xFFFFFF);
    /** Muted / secondary text. */
    public static final Color TEXT_MUTED   = new Color(0xADADAD);
    /** Table row selection fill. */
    public static final Color SELECTION_BG = new Color(0x1C3B5E);
    /** Table horizontal grid line color. */
    public static final Color GRID_LINE    = new Color(0x333333);
    /** Table header background. */
    public static final Color HEADER_BG    = new Color(0x242424);

    // ── Status / Semantic Colors ───────────────────────────────────────────────
    /** Success green. */
    public static final Color STATUS_SUCCESS  = new Color(0x54B054);
    /** Warning amber. */
    public static final Color STATUS_WARNING  = new Color(0xFCB900);
    /** Neutral blue. */
    public static final Color STATUS_INFO     = new Color(0x0D7CF2);
    /** Muted grey. */
    public static final Color STATUS_NEUTRAL  = new Color(0x888888);
    /** Error red. */
    public static final Color STATUS_ERROR    = new Color(0xE74C3C);

    // ── Typography ────────────────────────────────────────────────────────────
    private static final String FONT_FAMILY = resolveFont();

    /** Base body text (14px). */
    public static final Font FONT_BODY      = new Font(FONT_FAMILY, Font.PLAIN,  14);
    /** Semibold label text (14px). */
    public static final Font FONT_SEMIBOLD  = new Font(FONT_FAMILY, Font.BOLD,   14);
    /** Small caption text (12px). */
    public static final Font FONT_CAPTION   = new Font(FONT_FAMILY, Font.PLAIN,  12);
    /** Section header / title (16px bold). */
    public static final Font FONT_TITLE     = new Font(FONT_FAMILY, Font.BOLD,   16);
    /** App-level title (18px bold). */
    public static final Font FONT_APP_TITLE = new Font(FONT_FAMILY, Font.BOLD,   18);
    /** Large accent numbers / totals. */
    public static final Font FONT_LARGE     = new Font(FONT_FAMILY, Font.BOLD,   20);

    // ── Geometry ──────────────────────────────────────────────────────────────
    /** Standard content padding on each side (px). */
    public static final int PADDING         = 16;
    /** Compact padding for small gaps (px). */
    public static final int PADDING_SM      = 8;
    /** Standard JTable row height (px). */
    public static final int ROW_HEIGHT      = 36;

    // ── Private constructor: static-only utility ───────────────────────────────
    private FluentTheme() {}

    /**
     * Resolves the best available Segoe UI family name on the current platform.
     * Falls back to SansSerif if Segoe is unavailable (Linux / macOS).
     *
     * @return the resolved font family name
     */
    private static String resolveFont() {
        for (String family : GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames()) {
            if ("Segoe UI".equals(family)) return "Segoe UI";
        }
        return Font.SANS_SERIF;
    }

    // ── Global FlatLaf UI defaults ─────────────────────────────────────────────

    /**
     * Applies all Fluent 2 Dark token overrides to FlatLaf's UIManager defaults.
     * Must be called once after {@code FlatDarkLaf.setup()} and before any
     * Swing component is instantiated.
     */
    public static void applyGlobalDefaults() {
        UIManager.put("Panel.background",           CANVAS);
        UIManager.put("Panel.foreground",           TEXT_PRIMARY);
        UIManager.put("Label.foreground",           TEXT_PRIMARY);
        UIManager.put("Label.font",                 FONT_BODY);

        UIManager.put("TextField.background",       INPUT_BG);
        UIManager.put("TextField.foreground",       TEXT_PRIMARY);
        UIManager.put("TextField.caretForeground",  TEXT_PRIMARY);
        UIManager.put("TextField.border",           inputBorder());
        UIManager.put("TextField.font",             FONT_BODY);

        UIManager.put("TextArea.background",        INPUT_BG);
        UIManager.put("TextArea.foreground",        TEXT_PRIMARY);
        UIManager.put("TextArea.caretForeground",   TEXT_PRIMARY);
        UIManager.put("TextArea.font",              FONT_BODY);

        UIManager.put("FormattedTextField.background",      INPUT_BG);
        UIManager.put("FormattedTextField.foreground",      TEXT_PRIMARY);
        UIManager.put("FormattedTextField.caretForeground", TEXT_PRIMARY);
        UIManager.put("FormattedTextField.font",            FONT_BODY);

        UIManager.put("Spinner.background",         INPUT_BG);
        UIManager.put("Spinner.foreground",         TEXT_PRIMARY);

        UIManager.put("ComboBox.background",        INPUT_BG);
        UIManager.put("ComboBox.foreground",        TEXT_PRIMARY);
        UIManager.put("ComboBox.selectionBackground", ACCENT_MUTED);
        UIManager.put("ComboBox.selectionForeground", TEXT_PRIMARY);
        UIManager.put("ComboBox.font",              FONT_BODY);

        UIManager.put("Button.background",          SURFACE_ALT);
        UIManager.put("Button.foreground",          TEXT_PRIMARY);
        UIManager.put("Button.font",                FONT_SEMIBOLD);
        UIManager.put("Button.hoverBackground",     BORDER);
        UIManager.put("Button.focusedBackground",   BORDER);
        UIManager.put("Button.pressedBackground",   INPUT_BG);
        UIManager.put("Button.arc",                 6);

        UIManager.put("RadioButton.background",     CANVAS);
        UIManager.put("RadioButton.foreground",     TEXT_PRIMARY);
        UIManager.put("RadioButton.font",           FONT_BODY);
        UIManager.put("CheckBox.background",        CANVAS);
        UIManager.put("CheckBox.foreground",        TEXT_PRIMARY);
        UIManager.put("CheckBox.font",              FONT_BODY);

        UIManager.put("TabbedPane.background",          CANVAS);
        UIManager.put("TabbedPane.foreground",          TEXT_MUTED);
        UIManager.put("TabbedPane.selectedForeground",  TEXT_PRIMARY);
        UIManager.put("TabbedPane.underlineColor",      ACCENT);
        UIManager.put("TabbedPane.font",                FONT_SEMIBOLD);
        UIManager.put("TabbedPane.tabInsets",           new Insets(10, 18, 10, 18));
        UIManager.put("TabbedPane.selectedBackground",  SURFACE);
        UIManager.put("TabbedPane.hoverColor",          SURFACE_ALT);

        UIManager.put("Table.background",           SURFACE);
        UIManager.put("Table.foreground",           TEXT_PRIMARY);
        UIManager.put("Table.gridColor",            GRID_LINE);
        UIManager.put("Table.selectionBackground",  SELECTION_BG);
        UIManager.put("Table.selectionForeground",  TEXT_PRIMARY);
        UIManager.put("Table.showHorizontalLines",  true);
        UIManager.put("Table.showVerticalLines",    false);
        UIManager.put("Table.font",                 FONT_BODY);
        UIManager.put("Table.rowHeight",            ROW_HEIGHT);
        UIManager.put("Table.alternateRowColor",    new Color(0x262626));

        UIManager.put("TableHeader.background",           HEADER_BG);
        UIManager.put("TableHeader.foreground",           TEXT_MUTED);
        UIManager.put("TableHeader.font",                 FONT_SEMIBOLD);
        UIManager.put("TableHeader.separatorColor",       BORDER);
        UIManager.put("TableHeader.bottomSeparatorColor", BORDER);

        UIManager.put("ScrollPane.background",      SURFACE);
        UIManager.put("ScrollPane.border",          BorderFactory.createLineBorder(BORDER, 1));
        UIManager.put("ScrollBar.background",       SURFACE);
        UIManager.put("ScrollBar.thumb",            BORDER);
        UIManager.put("ScrollBar.thumbArc",         999);
        UIManager.put("ScrollBar.width",            8);

        UIManager.put("SplitPane.background",       CANVAS);
        UIManager.put("SplitPaneDivider.draggingColor", BORDER);
        UIManager.put("SplitPane.dividerSize",      6);

        UIManager.put("Separator.foreground",       BORDER);
        UIManager.put("Separator.background",       BORDER);

        UIManager.put("OptionPane.background",      SURFACE);
        UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);
        UIManager.put("OptionPane.font",            FONT_BODY);

        UIManager.put("ToolTip.background",         SURFACE_ALT);
        UIManager.put("ToolTip.foreground",         TEXT_MUTED);
        UIManager.put("ToolTip.border",             BorderFactory.createLineBorder(BORDER));
        UIManager.put("ToolTip.font",               FONT_CAPTION);

        UIManager.put("Component.arc",              6);
        UIManager.put("Component.focusWidth",       2);
        UIManager.put("Component.focusColor",       ACCENT);
        UIManager.put("Component.innerFocusWidth",  0);
        UIManager.put("Component.borderColor",      BORDER);
        UIManager.put("Component.disabledBorderColor", new Color(0x2A2A2A));
        UIManager.put("TextComponent.arc",          6);
    }

    // ── Factory: Borders ──────────────────────────────────────────────────────

    /**
     * Creates a standard card border: 1px outline plus 16px inner padding.
     *
     * @return a compound card border
     */
    public static Border cardBorder() {
        return new CompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1),
            new EmptyBorder(PADDING, PADDING, PADDING, PADDING)
        );
    }

    /**
     * Creates a compact card border with 8px inner padding.
     *
     * @return a compact compound card border
     */
    public static Border cardBorderCompact() {
        return new CompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1),
            new EmptyBorder(PADDING_SM, PADDING_SM, PADDING_SM, PADDING_SM)
        );
    }

    /**
     * Creates the standard input field border: 1px solid outline plus inner padding.
     *
     * @return the input border
     */
    public static Border inputBorder() {
        return new CompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1),
            new EmptyBorder(5, 8, 5, 8)
        );
    }

    // ── Factory: Components ───────────────────────────────────────────────────

    /**
     * Creates a styled section header panel with a title label and bottom divider.
     *
     * @param title the section label text
     * @return a themed section header JPanel
     */
    public static JPanel sectionHeader(String title) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(SURFACE_ALT);
        panel.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER),
            new EmptyBorder(10, PADDING, 10, PADDING)
        ));
        JLabel label = new JLabel(title);
        label.setFont(FONT_SEMIBOLD);
        label.setForeground(TEXT_PRIMARY);
        panel.add(label, BorderLayout.WEST);
        return panel;
    }

    /**
     * Creates an accent (Fluent Blue) primary action button.
     *
     * @param text button label
     * @return a themed JButton
     */
    public static JButton accentButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(ACCENT);
        btn.setForeground(Color.WHITE);
        btn.setFont(FONT_SEMIBOLD);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(8, 18, 8, 18));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(ACCENT_HOVER);
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(ACCENT);
            }
        });
        return btn;
    }

    /**
     * Creates a standard secondary button with surface background.
     *
     * @param text button label
     * @return a themed JButton
     */
    public static JButton secondaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(SURFACE_ALT);
        btn.setForeground(TEXT_PRIMARY);
        btn.setFont(FONT_SEMIBOLD);
        btn.setFocusPainted(false);
        btn.setBorder(new CompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1),
            new EmptyBorder(7, 16, 7, 16)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(BORDER);
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(SURFACE_ALT);
            }
        });
        return btn;
    }

    /**
     * Creates a ghost / accent text button with no background fill.
     *
     * @param text icon or short label text
     * @return a themed ghost JButton
     */
    public static JButton ghostButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(CANVAS);
        btn.setForeground(ACCENT);
        btn.setFont(FONT_SEMIBOLD);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                if (btn.isEnabled()) btn.setForeground(ACCENT_HOVER);
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                if (btn.isEnabled()) btn.setForeground(ACCENT);
            }
        });
        return btn;
    }

    /**
     * Applies standard Fluent Dark styling to a JTextField.
     *
     * @param field the text field to style
     */
    public static void styleTextField(JTextField field) {
        field.setBackground(INPUT_BG);
        field.setForeground(TEXT_PRIMARY);
        field.setCaretColor(TEXT_PRIMARY);
        field.setFont(FONT_BODY);
        field.setBorder(inputBorder());
        field.setOpaque(true);
    }

    /**
     * Applies standard Fluent Dark styling to a JTextArea.
     *
     * @param area the text area to style
     */
    public static void styleTextArea(JTextArea area) {
        area.setBackground(INPUT_BG);
        area.setForeground(TEXT_PRIMARY);
        area.setCaretColor(TEXT_PRIMARY);
        area.setFont(FONT_BODY);
        area.setBorder(new EmptyBorder(6, 8, 6, 8));
        area.setOpaque(true);
    }

    /**
     * Applies standard Fluent Dark styling to a JComboBox.
     *
     * @param combo the combo box to style
     */
    @SuppressWarnings("rawtypes")
    public static void styleCombo(JComboBox combo) {
        combo.setBackground(INPUT_BG);
        combo.setForeground(TEXT_PRIMARY);
        combo.setFont(FONT_BODY);
    }

    /**
     * Applies full Fluent Dark styling to a JTable and its header.
     *
     * @param table the JTable to style
     */
    public static void styleTable(JTable table) {
        table.setBackground(SURFACE);
        table.setForeground(TEXT_PRIMARY);
        table.setGridColor(GRID_LINE);
        table.setSelectionBackground(SELECTION_BG);
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setRowHeight(ROW_HEIGHT);
        table.setFillsViewportHeight(true);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setFont(FONT_BODY);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.getTableHeader().setReorderingAllowed(false);

        JTableHeader header = table.getTableHeader();
        header.setBackground(HEADER_BG);
        header.setForeground(TEXT_MUTED);
        header.setFont(FONT_SEMIBOLD);
        header.setBorder(new MatteBorder(0, 0, 1, 0, BORDER));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 40));

        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            { setHorizontalAlignment(SwingConstants.LEFT); }
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object value, boolean sel, boolean focus, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(
                    t, value, sel, focus, row, col);
                lbl.setBackground(HEADER_BG);
                lbl.setForeground(TEXT_MUTED);
                lbl.setFont(FONT_SEMIBOLD);
                lbl.setBorder(new EmptyBorder(0, 12, 0, 12));
                lbl.setOpaque(true);
                return lbl;
            }
        });
    }

    /**
     * Creates a styled status bar panel.
     *
     * @param label the JLabel that will hold status text (caller retains reference)
     * @return the assembled status bar panel
     */
    public static JPanel statusBar(JLabel label) {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        bar.setBackground(new Color(0x1A1A1A));
        bar.setBorder(new MatteBorder(1, 0, 0, 0, BORDER));
        label.setFont(FONT_CAPTION);
        label.setForeground(TEXT_MUTED);
        bar.add(label);
        return bar;
    }

    /**
     * Adds a right-aligned label plus an input component as one row in a
     * GridBagLayout-based form panel.
     *
     * @param panel   target container
     * @param gbc     shared constraints
     * @param row     zero-based row number
     * @param labelTx label text
     * @param field   the input component
     */
    public static void addFormRow(JPanel panel, GridBagConstraints gbc,
                                   int row, String labelTx, JComponent field) {
        JLabel lbl = new JLabel(labelTx);
        lbl.setFont(FONT_BODY);
        lbl.setForeground(TEXT_MUTED);
        lbl.setHorizontalAlignment(SwingConstants.RIGHT);

        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(6, 0, 6, 10);
        panel.add(lbl, gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 0, 6, 0);
        panel.add(field, gbc);
    }
}
