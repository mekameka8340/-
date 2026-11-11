/*
 * Atmosphere mod - lighting overlay shader
 */

package com.shatteredpixel.shatteredpixeldungeon.atmosphere;

import com.watabou.glscripts.Script;
import com.watabou.glwrap.Uniform;
import com.watabou.noosa.NoosaScript;

//Draws the light map over the world using "modulate 2x" blending:
// result = destination * (2 * fragment), so a fragment of 0.5 leaves the scene untouched,
// lower values darken it and higher values brighten it (up to 2x).
public class AtmosphereScript extends NoosaScript {

	public Uniform uTint;
	public Uniform uScreen;
	public Uniform uVignette;

	public AtmosphereScript() {
		super();
		uTint     = uniform( "uTint" );
		uScreen   = uniform( "uScreen" );
		uVignette = uniform( "uVignette" );
	}

	public static AtmosphereScript get() {
		return Script.use( AtmosphereScript.class );
	}

	@Override
	public void lighting(float rm, float gm, float bm, float am, float ra, float ga, float ba, float aa) {
		//unused
	}

	@Override
	protected String shader() {
		return SHADER;
	}

	private static final String SHADER =

		"uniform mat4 uCamera;\n" +
		"uniform mat4 uModel;\n" +
		"attribute vec4 aXYZW;\n" +
		"attribute vec2 aUV;\n" +
		"varying vec2 vUV;\n" +
		"void main() {\n" +
		"  gl_Position = uCamera * uModel * aXYZW;\n" +
		"  vUV = aUV;\n" +
		"}\n" +

		"//\n" +

		"#ifdef GL_ES\n" +
		"  precision mediump float;\n" +
		"#endif\n" +
		"varying vec2 vUV;\n" +
		"uniform sampler2D uTex;\n" +
		"uniform vec4 uTint;\n" +
		"uniform vec2 uScreen;\n" +
		"uniform float uVignette;\n" +
		"void main() {\n" +
		//light map stores half of the brightness multiplier
		"  vec3 m = texture2D( uTex, vUV ).rgb * 2.0 * uTint.rgb;\n" +
		//vignette, based on the position on the physical screen
		"  vec2 d = gl_FragCoord.xy / uScreen - 0.5;\n" +
		"  d.x *= uScreen.x / uScreen.y;\n" +
		"  float r = length( d ) / length( vec2( 0.5 * uScreen.x / uScreen.y, 0.5 ) );\n" +
		"  m *= 1.0 - uVignette * smoothstep( 0.35, 1.05, r );\n" +
		"  gl_FragColor = vec4( clamp( m * 0.5, 0.0, 1.0 ), 1.0 );\n" +
		"}\n";
}
