import processing.core.PGraphics;
import java.util.*;
/** Hand-drawn 3 x 5 bitmap alphabet: no system fonts or antialiasing. */
public final class PixelFont {
    private static final Map<Character,String> GLYPHS=new HashMap<>();
    static {
        String chars="ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.,:!?/+-=<>%()[]_#'";
        String[] rows={
            "010101111101101","110101110101110","011100100100011","110101101101110","111100110100111","111100110100100","011100101101011","101101111101101","111010010010111","001001001101010","101101110101101","100100100100111","101111111101101","101111111111101","010101101101010","110101110100100","010101101111011","110101110101101","011100010001110","111010010010010","101101101101111","101101101101010","101101111111101","101101010101101","101101010010010","111001010100111",
            "111101101101111","010110010010111","110001010100111","110001010001110","101101111001001","111100110001110","011100111101111","111001010010010","111101111101111","111101111001110",
            "000000000000010","000000000010100","000010000010000","010010010000010","110001010000010","001001010100100","000010111010000","000000111000000","000111000111000","001010100010001","100010001010100","101001010100101","001010010010001","100010010010100","011010010010011","110010010010110","000000000000111","101111101111101","010010000000000"};
        for(int i=0;i<chars.length();i++)GLYPHS.put(chars.charAt(i),rows[i]);
    }
    public static int width(String text,int scale){return Math.max(0,text.length()*4*scale-scale);}
    public static void draw(PGraphics g,String text,float x,float y,int scale,int color){
        g.noStroke();g.fill(color);int xx=Math.round(x), yy=Math.round(y);
        for(char c:text.toUpperCase(Locale.ROOT).toCharArray()){
            String bits=GLYPHS.get(c);
            if(bits!=null)for(int i=0;i<15;i++)if(bits.charAt(i)=='1')g.rect(xx+(i%3)*scale,yy+(i/3)*scale,scale,scale);
            xx+=4*scale;
        }
    }
}
