package ch.epfl.telegram;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

public class MenuHandler {

    public static ArrayList<String> search(String text, List<String> args) {

        ArrayList <String> lines = new ArrayList<String>();
        try {
            URL u = new URL("https://menus.epfl.ch/cgi-bin/getMenus");
            InputStream stream=u.openStream();
            DataInputStream dataStream = new DataInputStream(new BufferedInputStream(stream));
            String s;
            String result="";
            while ((s=dataStream.readLine())!=null){
                s=s.replaceAll("\\s+","");

                //System.out.println(s);
                if(s.equals("<divclass=\"desc\">")){
                    String meal=dataStream.readLine().split(">")[1].split("<")[0];
                    result=result+meal+"\t\t\t\t";

                }
                if(s.equals("<divclass=\"resto\">")){
                    s=dataStream.readLine();
                    String restaurant=dataStream.readLine().split(">")[1].split("<")[0];
                    result=result+restaurant;
                    result = URLDecoder.decode(new String(result.getBytes("ISO-8859-1"), "UTF-8"),"UTF-8");
                    lines.add(result);
                    result="";

                }
            }

            stream.close();

        }catch (MalformedURLException e1){
            e1.printStackTrace();

        }catch (IOException e2){
            e2.printStackTrace();
        }

        return lines;
    }

}