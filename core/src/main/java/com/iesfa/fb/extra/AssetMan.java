package com.iesfa.fb.extra;

import static com.iesfa.fb.extra.Utils.ATLAS_MAP;
import static com.iesfa.fb.extra.Utils.BACKGROUND_IMAGE;

import static com.iesfa.fb.extra.Utils.FONT_FNT;
import static com.iesfa.fb.extra.Utils.FONT_PNG;
import static com.iesfa.fb.extra.Utils.MUSIC_BG;
import static com.iesfa.fb.extra.Utils.PIPE_BOTTOM;
import static com.iesfa.fb.extra.Utils.PIPE_TOP;
import static com.iesfa.fb.extra.Utils.SOUND_JUMP;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.*;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class AssetMan {

    private AssetManager assetManager;
    private TextureAtlas textureAtlas;

    public AssetMan() {
        this.assetManager = new AssetManager();

        assetManager.load(ATLAS_MAP, TextureAtlas.class);
        //2.1 Sound
        assetManager.load(SOUND_JUMP, Sound.class);
        //2.2 Music
        assetManager.load(MUSIC_BG, Music.class);
        assetManager.finishLoading();

        textureAtlas = assetManager.get(ATLAS_MAP);
    }

    public AtlasRegion getBackground(){
        return this.textureAtlas.findRegion(BACKGROUND_IMAGE);
    }

    public Animation<AtlasRegion> getBirdAnimation(){
        return new Animation<>(0.33f,
            textureAtlas.findRegion("bird1"),
            textureAtlas.findRegion("bird2"),
            textureAtlas.findRegion("bird3"));
    }

    public TextureRegion getPipeTop() {
        return  this.textureAtlas.findRegion(PIPE_TOP);
    }

    public TextureRegion getPipeBottom() {

          return this.textureAtlas.findRegion(PIPE_BOTTOM);
    }

    public Sound getJumpSound(){
        return this.assetManager.get(SOUND_JUMP);
    }

    public Music getMusicBG(){
        return this.assetManager.get(MUSIC_BG);
    }

    //Crear en utils las variables para los identificadores de los archivos.
    public BitmapFont getFont(){
        return new BitmapFont(Gdx.files.internal(FONT_FNT),Gdx.files.internal(FONT_PNG), false);
    }
}
