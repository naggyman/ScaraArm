/* Code for Assignment ?? 
 * Name:
 * Usercode:
 * ID:
 */

import ecs100.*;
import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.awt.image.DataBufferByte;

/** <description of class Main>
 */
public class Main{

    private Arm arm;
    private Drawing drawing;
    private ToolPath tool_path;
    // state of the GUI
    private int state; // 0 - nothing
    // 1 - inverse point kinematics - point
    // 2 - enter path. Each click adds point  
    // 3 - enter path pause. Click does not add the point to the path

    private String PI_IP = "192.168.fuckyou";
    /**      */
    public Main(){
        UI.initialise();
        UI.addButton("xy to angles", this::inverse);
        UI.addButton("Enter path XY", this::enter_path_xy);
        UI.addButton("Save path XY", this::save_xy);
        UI.addButton("Load path XY", this::load_xy);
        UI.addButton("Save path Ang", this::save_ang);
        UI.addButton("Load path Ang:Play", this::load_ang);
        UI.addButton("Save PWM", this::save_pwm);
        UI.addButton("Print PWM", this::print_pwm);
        UI.addButton("Transfer to Pi", this::transfer);
        UI.addButton("Convert Image to Drawing", this::convert);

        // UI.addButton("Quit", UI::quit);
        UI.setMouseMotionListener(this::doMouse);
        UI.setKeyListener(this::doKeys);

        //ServerSocket serverSocket = new ServerSocket(22); 
        this.arm = new Arm();
        this.drawing = new Drawing();
        this.tool_path = new ToolPath();
        this.run();
        arm.draw();
    }

    public void doKeys(String action){
        UI.printf("Key :%s \n", action);
        if (action.equals("b")) {
            // break - stop entering the lines
            state = 3;
            //

        }

    }

    public void doMouse(String action, double x, double y) {
        //UI.printf("Mouse Click:%s, state:%d  x:%3.1f  y:%3.1f\n",
        //   action,state,x,y);
        UI.clearGraphics();
        String out_str=String.format("%3.1f %3.1f",x,y);
        UI.drawString(out_str, x+10,y+10);
        // 
        if ((state == 1)&&(action.equals("clicked"))){
            // draw as 

            arm.inverseKinematic(x,y);
            arm.draw();
            return;
        }

        if ( ((state == 2)||(state == 3))&&action.equals("moved") ){
            // draw arm and path
            arm.inverseKinematic(x,y);
            arm.draw();

            // draw segment from last entered point to current mouse position
            if ((state == 2)&&(drawing.get_path_size()>0)){
                PointXY lp = new PointXY();
                lp = drawing.get_path_last_point();
                //if (lp.get_pen()){
                UI.setColor(Color.GRAY);
                UI.drawLine(lp.get_x(),lp.get_y(),x,y);
                // }
            }
            drawing.draw();
        }

        // add point
        if (   (state == 2) &&(action.equals("clicked"))){
            // add point(pen down) and draw
            UI.printf("Adding point x=%f y=%f\n",x,y);
            drawing.add_point_to_path(x,y,true); // add point with pen down

            arm.inverseKinematic(x,y);
            arm.draw();
            drawing.draw();
            drawing.print_path();
        }

        if (   (state == 3) &&(action.equals("clicked"))){
            // add point and draw
            //UI.printf("Adding point x=%f y=%f\n",x,y);
            drawing.add_point_to_path(x,y,false); // add point wit pen up

            arm.inverseKinematic(x,y);
            arm.draw();
            drawing.draw();
            drawing.print_path();
            state = 2;
        }

    }

    public void save_xy(){
        state = 0;
        String fname = UIFileChooser.save();
        drawing.save_path(fname);
    }

    public void enter_path_xy(){
        state = 2;
    }

    public void inverse(){
        state = 1;
        arm.draw();
    }

    public void load_xy(){
        state = 0;
        String fname = UIFileChooser.open();
        drawing.load_path(fname);
        drawing.draw();

        arm.draw();
    }

    // save angles into the file
    public void save_ang(){
        String fname = UIFileChooser.save();
        tool_path.convert_drawing_to_angles(drawing,arm,fname);
        tool_path.save_angles(fname);
    }

    public void load_ang(){

    }

    public void save_pwm(){
        String fname = UIFileChooser.save();
        tool_path.convert_drawing_to_angles(drawing,arm,fname);
        tool_path.convert_angles_to_pwm(arm);
        tool_path.save_pwm_file(fname);
    }

    public void print_pwm(){
        UI.println( arm.get_pwm1() + " " + arm.get_pwm2());
    }

    public void transfer(){
        String fname = "temp.pwm";
        tool_path.convert_drawing_to_angles(drawing,arm,fname);
        tool_path.convert_angles_to_pwm(arm);
        tool_path.save_pwm_file(fname);
        
        try{
            //String command = "ping -c 3 google.com";
            String command = "echo hai";
            StringBuffer output = new StringBuffer();
            Process p;
            p = Runtime.getRuntime().exec(command);
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = "";
            while((line = reader.readLine()) != null){
                output.append(line+"\n");
            }
            UI.println(output.toString());
        }
        catch(Exception e){
            UI.println("Error: " + e);
        }
    }

    public void run() {
        while(true) {
            arm.draw();
            UI.sleep(20);
        }
    }
    
    public void convert() {
        int tx = 230, ty = 80;
        File png = new File(UIFileChooser.open("Please choose an File from the list"));
        //Read hte image
        BufferedImage img=null;
        try {
            img = ImageIO.read(png);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Create a new greyscale image
        if(img==null)
            UI.println("img was null");

        BufferedImage newBufferedImage = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        newBufferedImage.createGraphics().drawImage(img, 0, 0, Color.WHITE, null);
        img = newBufferedImage;
        //Convert the image to an array of bytes
        byte[] pixels = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
        //Handle the pen position
        boolean cur = false, last = false;
        int dir = 1;

        //Go to the start of the image
        drawing.add_point_to_path(tx, ty, false);
        for (int y = 0; y < img.getHeight(); y++) {
            //Add the current point to the page
            drawing.add_point_to_path((dir == 1 ? 0 : img.getWidth()) + tx, y + ty, false);
            //Alternate direction so that the pen doesn't cross the page
            for (int x = dir == 1 ? 0 : img.getWidth() - 1; x >= 0 && x < img.getWidth(); x += dir) {
                cur = pixels[(y * img.getWidth()) + x] >= 0;
                //We only care when the pixels are different to the last drawn pixel
                if (cur != last) {
                    if (cur) {
                        drawing.add_point_to_path(x + tx, y + ty, true);
                    } else {
                        drawing.add_point_to_path((x - dir) + tx, y + ty, false);
                    }
                }
                last = cur;
            }
            //Finishing the stroke at the end of the page
            if (cur && dir == 1) {
                drawing.add_point_to_path(img.getWidth() + tx, y + ty, true);
            }
            if (cur && dir == -1) {
                drawing.add_point_to_path(tx, y + ty, true);
            }
            dir = -dir;
        }
        drawing.draw();
    }

    public static void main(String[] args){
        Main obj = new Main();
    }    

}
