package com.etheller.warsmash;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.etheller.warsmash.util.FramePacingTracker;
import com.etheller.warsmash.util.StartupDiagnostics;
import com.etheller.warsmash.viewer5.CanvasProvider;

public class WarsmashGdxMultiScreenGame extends Game implements CanvasProvider {

	private final FramePacingTracker framePacingTracker = new FramePacingTracker();

	@Override
	public void create() {
		StartupDiagnostics.reportGLCapabilities();
	}

	@Override
	public void render() {
		framePacingTracker.tick(Gdx.graphics.getRawDeltaTime());
		super.render();
	}

	@Override
	public float getWidth() {
		return Gdx.graphics.getWidth();
	}

	@Override
	public float getHeight() {
		return Gdx.graphics.getHeight();
	}

}
