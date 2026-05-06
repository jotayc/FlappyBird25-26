package com.iesfa.fb.extra;

import static com.iesfa.fb.extra.Utils.ATLAS_MAP;
import static com.iesfa.fb.extra.Utils.BACKGROUND_IMAGE;

import static com.iesfa.fb.extra.Utils.PIPE_BOTTOM;
import static com.iesfa.fb.extra.Utils.PIPE_TOP;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.*;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class AssetMan {

    private AssetManager assetManager;
    private TextureAtlas textureAtlas;



    public AssetMan() {
        this.assetManager = new AssetManager();

        assetManager.load(ATLAS_MAP, TextureAtlas.class);
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



}
