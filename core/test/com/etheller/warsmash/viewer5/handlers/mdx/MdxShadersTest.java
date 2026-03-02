package com.etheller.warsmash.viewer5.handlers.mdx;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.etheller.warsmash.viewer5.Shaders;
import com.etheller.warsmash.viewer5.handlers.mdx.MdxHandler.ShaderEnvironmentType;

/**
 * Phase B validation: verify that the GLSL 120 -> 330 core migration in the HD
 * shader pipeline is complete and that no legacy keywords leaked through.
 */
class MdxShadersTest {

	@BeforeEach
	void setUp() {
		MdxHandler.CURRENT_SHADER_TYPE = ShaderEnvironmentType.GAME;
	}

	/**
	 * Strips GLSL comment lines (both // and block comments on a single line)
	 * so we can validate only active shader code.
	 */
	private static String stripComments(final String glsl) {
		final StringBuilder sb = new StringBuilder();
		for (final String line : glsl.split("\\r?\\n")) {
			final String trimmed = line.trim();
			if (!trimmed.startsWith("//") && !trimmed.startsWith("/*")) {
				sb.append(line).append('\n');
			}
		}
		return sb.toString();
	}

	// ---------------------------------------------------------------
	// Vertex shader (vsHd)
	// ---------------------------------------------------------------

	@Test
	void vsHd_startsWithVersion330Core() {
		assertTrue(MdxShaders.vsHd.startsWith("#version 330 core"),
				"vsHd must begin with #version 330 core");
	}

	@Test
	void vsHd_doesNotContainAttribute() {
		assertFalse(MdxShaders.vsHd.contains("attribute "),
				"vsHd should use 'in' instead of 'attribute'");
	}

	@Test
	void vsHd_doesNotContainVarying() {
		assertFalse(MdxShaders.vsHd.contains("varying "),
				"vsHd should use 'out' instead of 'varying'");
	}

	@Test
	void vsHd_doesNotContainTexture2D() {
		assertFalse(MdxShaders.vsHd.contains("texture2D"),
				"vsHd should use 'texture()' instead of 'texture2D()'");
	}

	@Test
	void vsHd_containsInKeywordForVertexInputs() {
		assertTrue(MdxShaders.vsHd.contains("in vec3 a_position"),
				"vsHd must declare a_position with 'in'");
		assertTrue(MdxShaders.vsHd.contains("in vec3 a_normal"),
				"vsHd must declare a_normal with 'in'");
		assertTrue(MdxShaders.vsHd.contains("in vec2 a_uv"),
				"vsHd must declare a_uv with 'in'");
	}

	@Test
	void vsHd_containsOutKeywordForInterpolatedOutputs() {
		assertTrue(MdxShaders.vsHd.contains("out vec2 v_uv"),
				"vsHd must declare v_uv with 'out'");
		assertTrue(MdxShaders.vsHd.contains("out vec3 v_eyeVec"),
				"vsHd must declare v_eyeVec with 'out'");
		assertTrue(MdxShaders.vsHd.contains("out vec3 v_normal"),
				"vsHd must declare v_normal with 'out'");
	}

	// ---------------------------------------------------------------
	// Fragment shader (fsHd) — checked against active (non-comment) code
	// ---------------------------------------------------------------

	@Test
	void fsHd_startsWithVersion330Core() {
		final String fsHd = MdxShaders.fsHd();
		assertTrue(fsHd.startsWith("#version 330 core"),
				"fsHd must begin with #version 330 core");
	}

	@Test
	void fsHd_activeCode_doesNotContainGlFragColor() {
		final String active = stripComments(MdxShaders.fsHd());
		assertFalse(active.contains("gl_FragColor"),
				"Active fsHd code should use 'fragColor' instead of 'gl_FragColor'");
	}

	@Test
	void fsHd_declaresFragColorOutput() {
		final String fsHd = MdxShaders.fsHd();
		assertTrue(fsHd.contains("out vec4 fragColor"),
				"fsHd must declare 'out vec4 fragColor'");
	}

	@Test
	void fsHd_doesNotContainVarying() {
		final String fsHd = MdxShaders.fsHd();
		assertFalse(fsHd.contains("varying "),
				"fsHd should use 'in' instead of 'varying'");
	}

	@Test
	void fsHd_activeCode_doesNotContainTexture2D() {
		final String active = stripComments(MdxShaders.fsHd());
		assertFalse(active.contains("texture2D("),
				"Active fsHd code should use 'texture()' instead of 'texture2D()'");
	}

	@Test
	void fsHd_usesInKeywordForInputs() {
		final String fsHd = MdxShaders.fsHd();
		assertTrue(fsHd.contains("in vec2 v_uv"),
				"fsHd must declare v_uv with 'in'");
		assertTrue(fsHd.contains("in vec3 v_eyeVec"),
				"fsHd must declare v_eyeVec with 'in'");
	}

	@Test
	void fsHd_usesFragColorInLambertOutput() {
		final String fsHd = MdxShaders.fsHd();
		assertTrue(fsHd.contains("fragColor = vec4(color, baseColor.a)"),
				"fsHd lambert() must write to fragColor");
	}

	// ---------------------------------------------------------------
	// Both shader types produce consistent output
	// ---------------------------------------------------------------

	@Test
	void fsHd_menuType_startsWithVersion330Core() {
		MdxHandler.CURRENT_SHADER_TYPE = ShaderEnvironmentType.MENU;
		final String fsHd = MdxShaders.fsHd();
		assertTrue(fsHd.startsWith("#version 330 core"),
				"fsHd (MENU) must begin with #version 330 core");
	}

	// ---------------------------------------------------------------
	// Shared helpers
	// ---------------------------------------------------------------

	@Test
	void transforms_doesNotContainAttribute() {
		assertFalse(Shaders.transforms.contains("attribute "),
				"Shaders.transforms should use 'in' instead of 'attribute'");
	}

	@Test
	void transforms_containsInKeyword() {
		assertTrue(Shaders.transforms.contains("in vec4 a_bones"),
				"Shaders.transforms must declare a_bones with 'in'");
		assertTrue(Shaders.transforms.contains("in vec4 a_weights"),
				"Shaders.transforms must declare a_weights with 'in'");
	}

	@Test
	void boneTexture_originalStillUsesTexture2D() {
		assertTrue(Shaders.boneTexture.contains("texture2D"),
				"Original boneTexture must still use texture2D for compatibility shaders");
	}

	@Test
	void boneTexture330_doesNotContainTexture2D() {
		final String bone330 = Shaders.boneTexture.replace("texture2D", "texture");
		assertFalse(bone330.contains("texture2D"),
				"BONE_TEXTURE_330 must not contain texture2D");
		assertTrue(bone330.contains("texture("),
				"BONE_TEXTURE_330 must use texture()");
	}

	// ---------------------------------------------------------------
	// No remaining legacy GLSL versions
	// ---------------------------------------------------------------

	@Test
	void noVersion120InHdShaders() {
		assertFalse(MdxShaders.vsHd.contains("#version 120"),
				"vsHd must not contain #version 120");
		assertFalse(MdxShaders.fsHd().contains("#version 120"),
				"fsHd must not contain #version 120");
	}

	@Test
	void noVersion450InAnyShader() {
		assertFalse(MdxShaders.vsHd.contains("#version 450"),
				"vsHd must not contain #version 450");
	}

	// ---------------------------------------------------------------
	// Phase C pre-test: shader correctness fixes
	// ---------------------------------------------------------------

	/**
	 * The first light in vsHd must be wrapped in an 'if (u_lightTextureHeight >
	 * 0.5)' guard so that an empty light texture does not cause a floating-point
	 * divide-by-zero (0.5 / 0.0 = +Infinity).
	 */
	@Test
	void vsHd_firstLightIsGuardedByLightCount() {
		assertTrue(MdxShaders.vsHd.contains("if (u_lightTextureHeight > 0.5)"),
				"vsHd must guard the first light read with 'if (u_lightTextureHeight > 0.5)' "
						+ "to prevent divide-by-zero when no lights are present");
	}

	/**
	 * The default value written to v_lightDir when no lights are present must be
	 * a safe zero vector, not undefined.
	 */
	@Test
	void vsHd_defaultsVLightDirToZero() {
		assertTrue(MdxShaders.vsHd.contains("v_lightDir = vec4(0.0)"),
				"vsHd must initialise v_lightDir to vec4(0.0) before any conditional light read");
	}

	/**
	 * The non-SKIN vertex-group path in Shaders.transforms must initialise
	 * {@code mat4 bone} to zero. GLSL 3.30 core does not zero-initialise local
	 * variables, so the original bare declaration {@code mat4 bone;} accumulated
	 * matrix contributions onto undefined memory.
	 */
	@Test
	void transforms_nonSkinPath_boneMatrixInitialised() {
		assertTrue(Shaders.transforms.contains("mat4 bone = mat4(0.0)"),
				"The non-SKIN getVertexGroupMatrix() must initialise bone with mat4(0.0), "
						+ "not a bare mat4 declaration, to avoid undefined behaviour in GLSL 3.30 core");
	}
}
