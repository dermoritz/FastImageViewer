import org.junit.Test;

import de.moritz.fastimageviewer.main.Main;

/**
 * Created by moritz on 25.02.2016.
 */
public class RunMain {

    @Test
    public void runMainOnImageService(){
        Main.main(new String[]{"http://server/image"});
    }

    @Test
    public void runMainEmpty(){
        Main.main(new String[]{});
    }


}
