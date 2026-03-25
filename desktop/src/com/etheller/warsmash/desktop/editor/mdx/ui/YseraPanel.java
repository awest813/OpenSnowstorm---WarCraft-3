package com.etheller.warsmash.desktop.editor.mdx.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Toolkit;
import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl.LwjglCanvas;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.etheller.warsmash.WarsmashPreviewApplication;
import com.etheller.warsmash.desktop.editor.mdx.listeners.YseraGUIListener;
import com.etheller.warsmash.desktop.editor.util.ExceptionPopup;
import com.etheller.warsmash.viewer5.handlers.w3x.camera.PortraitCameraManager;
import com.hiveworkshop.rms.parsers.mdlx.MdlxModel;

public class YseraPanel extends JPanel {
	private static final Quaternion IDENTITY = new Quaternion();
	private final WarsmashPreviewApplication warsmashPreviewApplication;
	private final JFileChooser userFileChooser = new JFileChooser();
	private final YseraGUIListener.YseraGUINotifier notifier = new YseraGUIListener.YseraGUINotifier();

	private MdlxModel model;

	private AnimationControllerFrame animationControllerFrame;

	public YseraPanel(final WarsmashPreviewApplication warsmashPreviewApplication) {
		this.warsmashPreviewApplication = warsmashPreviewApplication;
		setLayout(new BorderLayout());
		final LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.useGL30 = true;
		config.gles30ContextMajorVersion = 3;
		config.gles30ContextMinorVersion = 3;
		final LwjglCanvas lwjglCanvas = new LwjglCanvas(warsmashPreviewApplication, config);
		add(BorderLayout.CENTER, lwjglCanvas.getCanvas());
		setPreferredSize(new Dimension(640, 480));
		this.userFileChooser
				.setFileFilter(new FileNameExtensionFilter("Warcraft III Model or Texture", "mdx", "mdl", "blp"));

		final CameraMouseHandler cameraMouseHandler = new CameraMouseHandler(warsmashPreviewApplication);
		Gdx.input.setInputProcessor(cameraMouseHandler);

	}

	public JMenuBar createJMenuBar(final JFrame frame) {
		final JMenuBar jMenuBar = new JMenuBar();

		final JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic(KeyEvent.VK_F);
		fileMenu.setToolTipText("File Operations");
		fileMenu.getAccessibleContext().setAccessibleName("File Operations");
		final JMenuItem openItem = new JMenuItem("Open");
		openItem.setMnemonic(KeyEvent.VK_O);
		openItem.setToolTipText("Open Model");
		openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
		openItem.getAccessibleContext().setAccessibleName("Open Model");
		openItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				try {
					final int userResult = YseraPanel.this.userFileChooser.showOpenDialog(frame);
					if (userResult == JFileChooser.APPROVE_OPTION) {
						final File selectedFile = YseraPanel.this.userFileChooser.getSelectedFile();
						if (selectedFile != null) {
							YseraPanel.this.model = YseraPanel.this.warsmashPreviewApplication
									.loadCustomModel(selectedFile.getPath());
							YseraPanel.this.notifier.openModel(YseraPanel.this.model);
						}
					}
				}
				catch (final Exception exc) {
					ExceptionPopup.display(exc);
				}
			}
		});
		fileMenu.add(openItem);
		jMenuBar.add(fileMenu);
		final JMenu recentFilesMenu = new JMenu("Recent Files");
		recentFilesMenu.setMnemonic(KeyEvent.VK_R);
		recentFilesMenu.setToolTipText("Recent Files");
		recentFilesMenu.getAccessibleContext().setAccessibleName("Recent Files");
		jMenuBar.add(recentFilesMenu);
		final JMenu editMenu = new JMenu("Edit");
		editMenu.setMnemonic(KeyEvent.VK_E);
		editMenu.setToolTipText("Edit");
		editMenu.getAccessibleContext().setAccessibleName("Edit");
		jMenuBar.add(editMenu);
		final JMenu viewMenu = new JMenu("View");
		viewMenu.setMnemonic(KeyEvent.VK_V);
		viewMenu.setToolTipText("View");
		viewMenu.getAccessibleContext().setAccessibleName("View");
		jMenuBar.add(viewMenu);
		final JMenu teamColorMenu = new JMenu("Team Color");
		teamColorMenu.setMnemonic(KeyEvent.VK_T);
		teamColorMenu.setToolTipText("Team Color");
		teamColorMenu.getAccessibleContext().setAccessibleName("Team Color");
		jMenuBar.add(teamColorMenu);
		final JMenu windowMenu = new JMenu("Windows");
		windowMenu.setMnemonic(KeyEvent.VK_W);
		windowMenu.setToolTipText("Windows");
		windowMenu.getAccessibleContext().setAccessibleName("Windows");
		final JMenuItem modelEditorItem = new JMenuItem("Model Editor");
		modelEditorItem.setMnemonic(KeyEvent.VK_M);
		modelEditorItem.setToolTipText("Model Editor");
		modelEditorItem.getAccessibleContext().setAccessibleName("Model Editor");
		windowMenu.add(modelEditorItem);
		final JMenuItem animationControllerItem = new JMenuItem("Animation Controller");
		animationControllerItem.setMnemonic(KeyEvent.VK_A);
		animationControllerItem.setToolTipText("Animation Controller");
		animationControllerItem.getAccessibleContext().setAccessibleName("Animation Controller");
		animationControllerItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				if (YseraPanel.this.animationControllerFrame == null) {
					YseraPanel.this.animationControllerFrame = new AnimationControllerFrame(
							YseraPanel.this.warsmashPreviewApplication);
					YseraPanel.this.notifier.subscribe(YseraPanel.this.animationControllerFrame);
					YseraPanel.this.animationControllerFrame.setLocationRelativeTo(frame);
				}
				YseraPanel.this.animationControllerFrame.setVisible(true);
				YseraPanel.this.animationControllerFrame.toFront();
			}
		});
		windowMenu.add(animationControllerItem);
		jMenuBar.add(windowMenu);
		final JMenu extrasMenu = new JMenu("Extras");
		extrasMenu.setMnemonic(KeyEvent.VK_X);
		extrasMenu.setToolTipText("Extras");
		extrasMenu.getAccessibleContext().setAccessibleName("Extras");
		jMenuBar.add(extrasMenu);
		final JMenu helpMenu = new JMenu("Help");
		helpMenu.setMnemonic(KeyEvent.VK_H);
		helpMenu.setToolTipText("Help");
		helpMenu.getAccessibleContext().setAccessibleName("Help");
		jMenuBar.add(helpMenu);

		return jMenuBar;
	}

	private static final class CameraMouseHandler extends InputAdapter {
		private int lastX, lastY;
		private final Vector3 screenDimension = new Vector3();
		private int button;
		private final WarsmashPreviewApplication warsmashPreviewApplication;

		public CameraMouseHandler(final WarsmashPreviewApplication warsmashPreviewApplication) {
			this.warsmashPreviewApplication = warsmashPreviewApplication;
		}

		@Override
		public boolean touchDown(final int screenX, final int screenY, final int pointer, final int button) {
			this.lastX = screenX;
			this.lastY = screenY;
			this.button = button;
			return false;
		}

		@Override
		public boolean touchDragged(final int screenX, final int screenY, final int pointer) {
			final int newX = screenX;
			final int newY = screenY;
			final int dx = newX - this.lastX;
			final int dy = newY - this.lastY;
			final PortraitCameraManager cameraManager = this.warsmashPreviewApplication.getCameraManager();
			if (this.button == Input.Buttons.RIGHT) {
				this.screenDimension.set(-1, 0, 0);
				this.screenDimension.unrotate(cameraManager.camera.viewProjectionMatrix);
				cameraManager.target.add(this.screenDimension.nor().scl(dx * 5));
				this.screenDimension.set(0, 1, 0);
				this.screenDimension.unrotate(cameraManager.camera.viewProjectionMatrix);
				cameraManager.target.add(this.screenDimension.nor().scl(dy * 5));
			}
			else if (this.button == Input.Buttons.LEFT) {
				cameraManager.horizontalAngle -= Math.toRadians(dx);
				cameraManager.verticalAngle -= Math.toRadians(dy);
			}
			this.lastX = newX;
			this.lastY = newY;
			return false;
		}

		@Override
		public boolean scrolled(final float amountX, final float amountY) {
			final PortraitCameraManager cameraManager = this.warsmashPreviewApplication.getCameraManager();
			cameraManager.distance += amountY * 100;
			return false;
		}

	}
}
