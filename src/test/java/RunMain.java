import de.moritz.fastimageviewer.main.Main;
import org.junit.Test;

/**
 * Created by moritz on 25.02.2016.
 */
public class RunMain {

    @Test
    public void runMainOnImageService(){
        Main.main(new String[]{"http://user1:0ljLYTheARvcMEbAmhEB@server/image"});
    }

    @Test
    public void runMainEmpty(){
        Main.main(new String[]{});
    }


}
