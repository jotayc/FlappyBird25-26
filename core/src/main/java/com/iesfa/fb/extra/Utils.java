package com.iesfa.fb.extra;

//Suele ser necesario tener una clase que se encarge de centralizar todos los datos comunes a los
// que se va a acceder durante  la aplicación y que se centralice en un sitio
public class Utils {

    //Todo: Deberíamos crear variables que definan los tamaños de pantalla
    public static final int SCREEN_HEIGHT = 800;
    public static final int SCREEN_WIDTH = 480;

    //Todo: Deberíamos crear variables que definan los tamaños de nuestro mundo
    public static final float WORLD_HEIGTH = 8f;
    public static final float WORLD_WIDTH = 4.8f;

    //Todo: Identificadores de assets
    public static final String ATLAS_MAP = "FBAtlas";
    public static final String BACKGROUND_IMAGE = "flappy_background";

    //Todo Identificadores de cuerpos (física)
    public static final String USER_BIRD = "bird";
}
