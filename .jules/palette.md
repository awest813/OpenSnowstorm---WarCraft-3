## 2024-05-20 - Java Swing Menu Mnemonics
**Learning:** Warsmash relies heavily on Java Swing for its desktop editor tools (like YseraPanel and TerrainEditorPanel). A common accessibility oversight in these tools is the lack of keyboard shortcuts (mnemonics) for standard top-level menus and common actions (like "File" and "Open").
**Action:** When improving UI accessibility in Java desktop apps within this codebase, always check if JMenu and JMenuItem instances have mnemonics set using `setMnemonic(KeyEvent.VK_...)` to enable `Alt+Key` navigation.
## 2024-03-15 - [Improve ExceptionPopup display in desktop utilities]
**Learning:** Raw JTextPanes in JOptionPanes can cause massive unscrollable error dialogs, making them difficult to close and read.
**Action:** When displaying exception stacktraces or long text in Swing desktop components, always wrap them in a constrained `JScrollPane` (e.g., 600x400) and set `setEditable(false)` for a better user experience.
