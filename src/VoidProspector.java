import processing.core.*;
import java.nio.file.*;
import java.util.*;

/** An original monochrome space-mining demo built with Processing Java. */
public class VoidProspector extends PApplet {
    static final int W=640,H=360, INK=0xffe0e5cf, MUTED=0xff788273, DARK=0xff303a32, BG=0xff0b110f;
    static final String[] SECTORS={"THE SHALLOWS","SHATTERED BELT","THE HOLLOW STAR"};
    static final String[] DESCRIPTIONS={"A QUIET GRAVEYARD. SOMETHING STILL SHINES.","RICH VEINS. COOLANT VENTS. KEEP MOVING.","THE LAST SIGNAL. A STAR THAT PULSES RADIATION."};
    PGraphics g; GameState state=new GameState();
    String screen="TITLE", previous="FLIGHT", notice="", saveError="";
    boolean[] held=new boolean[512]; boolean click, particlesOn=true, shakeOn=true;
    boolean smoke=Boolean.getBoolean("void.smoke");
    int mx,my, selected=0, hoverBranch=-1, hoverTier=-1, tick, runOre, runCores, shotsFired;
    float px=700,py=500,vx,vy,hull=100,energy=85,aim,camX,camY,dt,now,last,noticeTime,invulnerable,shake,sectorTime;
    final float baseX=700,baseY=500;
    ArrayList<Rock> rocks=new ArrayList<>(); ArrayList<Drone> drones=new ArrayList<>();
    ArrayList<Drop> drops=new ArrayList<>(); ArrayList<Particle> sparks=new ArrayList<>(); ArrayList<Bolt> bolts=new ArrayList<>();
    Random world=new Random();
    Path savePath=Paths.get(System.getProperty("void.save",Paths.get(System.getProperty("user.home"),".voidprospector","save.properties").toString()));
    boolean hasSave; Rock targeted; Drone targetDrone;
    public static void main(String[] args){PApplet.main(VoidProspector.class.getName());}
    public void settings(){size(1280,720,JAVA2D);noSmooth();}
    public void setup(){
        surface.setTitle("VOID PROSPECTOR / Processing Java demo");frameRate(60);
        g=createGraphics(W,H,JAVA2D);g.noSmooth();hasSave=Files.exists(savePath);
        if(hasSave)try{state=GameState.load(savePath);}catch(Exception e){saveError="SAVE UNREADABLE - NEW RUN AVAILABLE";hasSave=false;}
        last=millis()/1000f;generateWorld();
    }
    public void draw(){
        now=millis()/1000f;dt=Math.min(.04f,Math.max(.001f,now-last));last=now;tick++;
        int scale=Math.max(1,Math.min(width/W,height/H)), ox=(width-W*scale)/2,oy=(height-H*scale)/2;
        mx=(mouseX-ox)/scale;my=(mouseY-oy)/scale;
        if(noticeTime>0)noticeTime-=dt;
        g.beginDraw();g.background(BG);g.noSmooth();
        if(screen.equals("FLIGHT")){updateFlight();drawFlight();}
        else if(screen.equals("BASE"))drawBase();
        else if(screen.equals("TITLE"))drawTitle();
        else if(screen.equals("CONFIRM"))drawConfirm();
        else if(screen.equals("HELP"))drawHelp();
        else if(screen.equals("SETTINGS"))drawSettings();
        else if(screen.equals("PAUSE")){drawFlight();drawPause();}
        else if(screen.equals("LOST")){drawFlight();drawLost();}
        else if(screen.equals("VICTORY"))drawVictory();
        if(noticeTime>0&&!screen.equals("TITLE")){int ww=PixelFont.width(notice,1)+20;panel((W-ww)/2,326,ww,19);center(notice,336,1,INK);}
        if(!saveError.isEmpty()){text(saveError,12,350,1,INK);}
        g.endDraw();background(BG);image(g,ox,oy,W*scale,H*scale);click=false;
        if(smoke)smokeSequence();
    }
    void text(String s,float x,float y,int size,int c){PixelFont.draw(g,s,x,y,size,c);}
    void center(String s,int y,int size,int c){text(s,(W-PixelFont.width(s,size))/2,y,size,c);}
    void line(float x,float y,float xx,float yy,int c){g.stroke(c);g.strokeWeight(1);g.line(Math.round(x),Math.round(y),Math.round(xx),Math.round(yy));}
    void box(float x,float y,float w,float h,int c){g.noStroke();g.fill(c);g.rect(Math.round(x),Math.round(y),w,h);}
    void border(float x,float y,float w,float h,int c){g.noFill();g.stroke(c);g.strokeWeight(1);g.rect(Math.round(x),Math.round(y),w,h);}
    void panel(int x,int y,int w,int h){box(x,y,w,h,BG);border(x,y,w,h,DARK);}
    boolean over(int x,int y,int w,int h){return mx>=x&&mx<x+w&&my>=y&&my<y+h;}
    boolean button(String label,int x,int y,int w,int h,boolean active){
        boolean hover=over(x,y,w,h);box(x,y,w,h,active&&hover?INK:BG);border(x,y,w,h,active?INK:DARK);
        text(label,x+(w-PixelFont.width(label,1))/2,y+(h-5)/2,1,active?(hover?BG:INK):MUTED);
        return active&&hover&&click;
    }
    void stars(float cx,float cy){
        for(int i=0;i<130;i++){
            int sx=(int)(((i*193L+17)%1543-cx*.12f)%W);if(sx<0)sx+=W;
            int sy=(int)(((i*277L+41)%947-cy*.12f)%H);if(sy<0)sy+=H;
            box(sx,sy,i%19==0?2:1,1,i%7==0?MUTED:DARK);
            if(i%29==0){line(sx-2,sy,sx+2,sy,DARK);line(sx,sy-2,sx,sy+2,DARK);}
        }
    }
    void drawTitle(){
        stars(now*5,0);text("OBSIDIAN SIGNAL / FIELD DEMO 01",24,21,1,MUTED);
        text("VOID",35,62,6,INK);text("PROSPECTOR",36,103,4,INK);
        text("THERE IS STILL SOMETHING OUT THERE.",38,140,1,MUTED);
        line(38,159,293,159,DARK);
        if(button(hasSave?"CONTINUE EXPEDITION":"BEGIN EXPEDITION",38,180,224,27,true)){screen="BASE";toast("WELCOME TO ANCHOR STATION");}
        if(hasSave&&button("NEW EXPEDITION",38,216,224,25,true)){screen="RESET";}
        if(button("FLIGHT MANUAL",38,hasSave?250:216,224,25,true)){previous="TITLE";screen="HELP";}
        if(button("SETTINGS",38,hasSave?284:250,224,25,true)){previous="TITLE";screen="SETTINGS";}
        // Large hand-drawn pixel ship and asteroid diorama.
        g.pushMatrix();g.translate(453,165);g.scale(5);rockSprite(16,9,11,0,42);shipSprite(-10,0,-.35f,false);g.popMatrix();
        text("03 SECTORS",399,248,1,INK);text("42 UPGRADES",399,261,1,INK);text("ONE WAY HOME",399,274,1,MUTED);
        line(24,329,616,329,DARK);text("WASD / MOVE    MOUSE / MINE    E / DOCK",24,342,1,MUTED);text("PROCESSING / JAVA",548,342,1,MUTED);
        if(screen.equals("RESET")){screen="CONFIRM";}
    }
    void drawConfirm(){
        stars(0,0);center("START A NEW EXPEDITION?",117,2,INK);
        center("THIS REPLACES YOUR STATION SAVE.",151,1,MUTED);
        if(button("YES / NEW RUN",175,191,140,28,true)){state=new GameState();generateWorld();save();screen="BASE";}
        if(button("NO / GO BACK",327,191,140,28,true))screen="TITLE";
        center("Y / CONFIRM     N / CANCEL",245,1,MUTED);
    }
    void drawHelp(){
        stars(0,0);text("FLIGHT MANUAL",28,26,3,INK);text("ANCHOR STATION / PILOT BRIEFING",29,52,1,MUTED);
        String[][] rows={{"01 / MOVE","WASD OR ARROW KEYS. SHIFT TO BOOST."},{"02 / EXTRACT","AIM WITH MOUSE. HOLD LEFT BUTTON TO CUT ROCKS."},{"03 / RECOVER","ORE IS PULLED INTO YOUR HOLD. CORES ARE SEPARATE."},{"04 / RETURN","FOLLOW THE STATION MARKER. PRESS E NEAR THE BASE."},{"05 / UPGRADE","DOCKING SELLS CARGO AND RESTORES HULL + ENERGY."},{"06 / DESCEND","BANK THE ORE + CORES SHOWN. JUMP AT THE STATION."},{"07 / SURVIVE","CUT DRONES WITH YOUR BEAM. DODGE THEIR BOLTS."},{"08 / REMEMBER","ESC PAUSES. SAVED AT THE STATION AND ON PURCHASE."}};
        for(int i=0;i<rows.length;i++){int y=82+i*27;text(rows[i][0],30,y,1,INK);text(rows[i][1],162,y,1,MUTED);line(30,y+17,610,y+17,DARK);}
        text("LOSS OF HULL: UNSOLD CARGO IS LOST. BANKED PROGRESS IS SAFE.",30,306,1,INK);
        if(button("BACK",30,327,95,21,true))screen=previous;
    }
    void drawSettings(){
        stars(0,0);text("SYSTEM SETTINGS",40,44,3,INK);text("PIXEL-PERFECT / INTEGER SCALING / NO FILTERING",41,78,1,MUTED);
        if(button("PARTICLES: "+(particlesOn?"ON":"OFF"),40,115,240,30,true))particlesOn=!particlesOn;
        if(button("SCREEN SHAKE: "+(shakeOn?"ON":"OFF"),40,158,240,30,true))shakeOn=!shakeOn;
        text("640 X 360 INTERNAL FRAMEBUFFER",41,215,1,INK);text("ORIGINAL PROCEDURAL PIXEL ART. TWO-TONE PHOSPHOR PALETTE.",41,233,1,MUTED);
        if(button("BACK",40,286,110,26,true))screen=previous;
    }
    void generateWorld(){
        rocks.clear();drones.clear();drops.clear();bolts.clear();sparks.clear();world.setSeed(92017+state.stage*377);
        int count=92+state.stage*28;
        for(int i=0;i<count;i++){
            float x=80+world.nextFloat()*1240,y=70+world.nextFloat()*860;
            if(dist(x,y,baseX,baseY)<120||inHazard(x,y)){i--;continue;}
            int type=i<6+state.stage*2?2:(i%4==0?1:0);
            rocks.add(new Rock(x,y,type,world.nextInt(100000)));
        }
        for(int i=0;i<3+state.stage*3;i++){
            float a=TWO_PI*i/(3+state.stage*3),r=330+world.nextFloat()*200;
            drones.add(new Drone(baseX+cos(a)*r,baseY+sin(a)*r));
        }
        px=baseX;py=baseY+48;vx=vy=0;camX=px-W/2;camY=py-H/2;
        hull=state.maxHull();energy=state.maxEnergy();runOre=runCores=0;invulnerable=2;sectorTime=0;
    }
    void launch(){
        screen="FLIGHT";hull=state.maxHull();energy=state.maxEnergy();
        px=baseX;py=baseY+60;vx=vy=0;Arrays.fill(held,false);toast("SECTOR 0"+(state.stage+1)+" / "+SECTORS[state.stage]);
    }
    void save(){try{state.save(savePath);hasSave=true;saveError="";}catch(Exception e){saveError="SAVE FAILED - CHECK SAVE DIRECTORY";System.err.println("Could not save: "+e.getMessage());}}
    void toast(String message){notice=message;noticeTime=3.5f;}
    boolean down(int k){return k>=0&&k<held.length&&held[k];}
    void updateFlight(){
        invulnerable=Math.max(0,invulnerable-dt);shake=Math.max(0,shake-dt*8);sectorTime+=dt;
        float ax=(down('D')||down(RIGHT)?1:0)-(down('A')||down(LEFT)?1:0);
        float ay=(down('S')||down(DOWN)?1:0)-(down('W')||down(UP)?1:0);
        float len=sqrt(ax*ax+ay*ay);if(len>0){ax/=len;ay/=len;}
        boolean boost=down(SHIFT)&&energy>8&&len>0;
        float speed=state.speed()*(boost?1.85f:1);vx=lerp(vx,ax*speed,Math.min(1,dt*6));vy=lerp(vy,ay*speed,Math.min(1,dt*6));
        px=constrain(px+vx*dt,18,1382);py=constrain(py+vy*dt,18,982);
        camX=lerp(camX,px-W/2,Math.min(1,dt*8));camY=lerp(camY,py-H/2,Math.min(1,dt*8));
        aim=atan2(my-(py-camY),mx-(px-camX));
        boolean firing=mousePressed&&mouseButton==LEFT&&energy>1&&my>43&&my<317;
        float reach=102+state.upgrades[0]*7;targeted=null;targetDrone=null;float nearest=reach;
        if(firing){
            for(Rock r:rocks)if(r.hp>0){float hit=rayCircle(r.x,r.y,r.radius);if(hit>=0&&hit<nearest){nearest=hit;targeted=r;targetDrone=null;}}
            for(Drone d:drones)if(d.hp>0){float hit=rayCircle(d.x,d.y,9);if(hit>=0&&hit<nearest){nearest=hit;targetDrone=d;targeted=null;}}
            energy=Math.max(0,energy-dt*22);
            if(targeted!=null){targeted.hp-=state.damage()*dt;emit(px+cos(aim)*nearest,py+sin(aim)*nearest,1);if(targeted.hp<=0)breakRock(targeted);}
            if(targetDrone!=null){targetDrone.hp-=state.damage()*dt;emit(targetDrone.x,targetDrone.y,1);if(targetDrone.hp<=0){emit(targetDrone.x,targetDrone.y,14);for(int i=0;i<4;i++)drops.add(new Drop(targetDrone.x,targetDrone.y,false));}}
        } else energy=Math.min(state.maxEnergy(),energy+dt*state.recharge());
        if(boost){energy=Math.max(0,energy-dt*14);emit(px-cos(aim)*10,py-sin(aim)*10,1);}
        if(!firing&&invulnerable<=0)hull=Math.min(state.maxHull(),hull+state.regen()*dt);
        if(firing&&energy<=1)toast("REACTOR LOW / RELEASE TO RECHARGE");
        for(Rock r:rocks)if(r.hp>0&&dist(px,py,r.x,r.y)<r.radius+5){
            float a=atan2(py-r.y,px-r.x);px=r.x+cos(a)*(r.radius+6);py=r.y+sin(a)*(r.radius+6);damage(7);vx*=.4;vy*=.4;
        }
        for(Drone d:drones)if(d.hp>0){
            float distance=dist(px,py,d.x,d.y);d.phase+=dt;float a=atan2(py-d.y,px-d.x);
            if(distance<330&&dist(px,py,baseX,baseY)>110){
                float move=distance>150?20+state.stage*7:-12;
                d.x+=(cos(a)*move+cos(a+HALF_PI)*15)*dt;d.y+=(sin(a)*move+sin(a+HALF_PI)*15)*dt;
                d.cooldown-=dt;if(d.cooldown<=0){bolts.add(new Bolt(d.x,d.y,a));d.cooldown=2.6f-state.stage*.4f;shotsFired++;}
                if(distance<15)damage(12);
            }
        }
        for(int i=bolts.size()-1;i>=0;i--){Bolt b=bolts.get(i);b.x+=b.vx*dt;b.y+=b.vy*dt;b.life-=dt;
            if(dist(b.x,b.y,baseX,baseY)<85)b.life=0;
            if(dist(b.x,b.y,px,py)<7){damage(10+state.stage*4);b.life=0;}
            if(b.life<=0)bolts.remove(i);
        }
        for(int i=drops.size()-1;i>=0;i--){Drop d=drops.get(i);d.age+=dt;
            float distance=dist(px,py,d.x,d.y);boolean room=d.core||runOre<state.capacity();
            if(distance<state.magnet()&&room){float a=atan2(py-d.y,px-d.x);d.x+=cos(a)*dt*150;d.y+=sin(a)*dt*150;}
            else{d.x+=d.vx*dt;d.y+=d.vy*dt;d.vx*=.95;d.vy*=.95;}
            if(distance<9&&room){if(d.core){runCores++;toast("SIGNAL CORE RECOVERED");}else runOre++;drops.remove(i);}
        }
        for(int i=sparks.size()-1;i>=0;i--){Particle p=sparks.get(i);p.life-=dt;p.x+=p.vx*dt;p.y+=p.vy*dt;if(p.life<=0)sparks.remove(i);}
        if(hazardActive()&&inHazard(px,py))damage(9+state.stage*3);
        if(down('E')&&dist(px,py,baseX,baseY)<96&&hull>0)dock();
        if(hull<=0){hull=0;screen="LOST";mousePressed=false;}
    }
    float rayCircle(float x,float y,float radius){
        float dx=x-px,dy=y-py,along=dx*cos(aim)+dy*sin(aim),across=abs(-dx*sin(aim)+dy*cos(aim));
        if(across>radius||along<0)return -1;
        return Math.max(0,along-sqrt(radius*radius-across*across));
    }
    void damage(float amount){if(invulnerable>0)return;hull-=amount*state.resistance();invulnerable=.6f;shake=2;emit(px,py,8);}
    void breakRock(Rock r){
        emit(r.x,r.y,18);shake=1;
        int amount=r.type==2?17:r.type==1?12:7;
        for(int i=0;i<amount;i++)drops.add(new Drop(r.x+random(-8,8),r.y+random(-8,8),false));
        if(r.type==2)drops.add(new Drop(r.x,r.y,true));
    }
    void emit(float x,float y,int n){if(!particlesOn)return;for(int i=0;i<n&&sparks.size()<300;i++)sparks.add(new Particle(x,y));}
    void dock(){int ore=runOre,cores=runCores;state.deposit(ore,cores);runOre=runCores=0;hull=state.maxHull();energy=state.maxEnergy();screen="BASE";save();toast("DEPOSITED "+ore+" ORE / "+cores+" CORES / +"+(state.value(ore)+cores*(20+state.stage*15))+" CR");}
    void drawFlight(){
        stars(camX,camY);float sx=shakeOn?sin(tick*3)*shake:0,sy=shakeOn?cos(tick*4)*shake:0;
        g.pushMatrix();g.translate(Math.round(-camX+sx),Math.round(-camY+sy));
        // Navigational grid, broken into unobtrusive points.
        for(int x=0;x<1400;x+=80)for(int y=0;y<1000;y+=80)box(x,y,1,1,DARK);
        border(0,0,1400,1000,DARK);drawSectorLandmarks();drawStation();
        for(Rock r:rocks)if(r.hp>0&&r.x>camX-35&&r.x<camX+W+35&&r.y>camY-35&&r.y<camY+H+35){
            rockSprite(r.x,r.y,r.radius,r.type,r.seed);
            if(r==targeted){bar((int)r.x-14,(int)r.y-r.radius-7,28,2,r.hp/r.maxHp,INK);}
        }
        for(Drop d:drops){int x=Math.round(d.x),y=Math.round(d.y);if(d.core){border(x-3,y-3,6,6,INK);box(x-1,y-1,2,2,INK);}else{box(x,y,2,2,INK);}}
        for(Drone d:drones)if(d.hp>0){
            float a=atan2(py-d.y,px-d.x);g.pushMatrix();g.translate(Math.round(d.x),Math.round(d.y));
            border(-5,-4,10,8,INK);box(-8,-2,3,4,MUTED);box(5,-2,3,4,MUTED);box(-2,-1,4,2,INK);
            line(cos(a)*6,sin(a)*6,cos(a)*12,sin(a)*12,INK);g.popMatrix();
            if(d.hp<d.maxHp)bar((int)d.x-7,(int)d.y-10,14,2,d.hp/d.maxHp,INK);
        }
        for(Bolt b:bolts){line(b.x,b.y,b.x-b.vx*.035f,b.y-b.vy*.035f,INK);box(b.x,b.y,2,2,INK);}
        for(Particle p:sparks)box(p.x,p.y,1,1,p.life>.25?INK:MUTED);
        boolean firing=screen.equals("FLIGHT")&&mousePressed&&mouseButton==LEFT&&energy>1&&my>43&&my<317;
        if(firing){float reach=102+state.upgrades[0]*7;float end=targeted!=null?Math.min(reach,dist(px,py,targeted.x,targeted.y)):targetDrone!=null?Math.min(reach,dist(px,py,targetDrone.x,targetDrone.y)):reach;
            line(px+cos(aim)*9,py+sin(aim)*9,px+cos(aim)*end,py+sin(aim)*end,INK);
            if(tick%3==0)line(px+cos(aim)*10,py+sin(aim)*10+1,px+cos(aim)*end,py+sin(aim)*end+1,MUTED);
        }
        if(invulnerable<=0||tick%6<3)shipSprite(px,py,aim,abs(vx)+abs(vy)>15);
        g.popMatrix();
        drawHud();
        float distBase=dist(px,py,baseX,baseY);
        if(distBase<96){panel(244,277,152,24);text("[E] DOCK AT ANCHOR",256,287,1,INK);}
        else{
            float a=atan2(baseY-py,baseX-px),xx=W/2+cos(a)*108,yy=H/2+sin(a)*78;
            text("BASE "+(int)distBase,xx-14,yy,1,MUTED);line(xx,yy-7,xx+cos(a)*7,yy-7+sin(a)*7,INK);
        }
        if(runOre>=state.capacity()){panel(226,247,188,18);text("HOLD FULL / RETURN TO BASE",238,254,1,INK);}
        if(screen.equals("FLIGHT")){line(mx-5,my,mx-2,my,INK);line(mx+2,my,mx+5,my,INK);line(mx,my-5,mx,my-2,INK);line(mx,my+2,mx,my+5,INK);}
    }
    boolean inHazard(float x,float y){
        if(state.stage==1)return dist(x,y,375,290)<65||dist(x,y,1060,720)<65;
        return state.stage==2&&dist(x,y,1110,250)<140;
    }
    boolean hazardActive(){return state.stage>0&&sectorTime%10>=7;}
    void drawSectorLandmarks(){
        if(state.stage==1){
            drawVent(375,290);drawVent(1060,720);
            // Broken orbital infrastructure in the second belt.
            for(int i=0;i<4;i++){int x=930+i*22,y=175+i*9;border(x,y,16,10,DARK);line(x,y,x+16,y+10,MUTED);}
            text("DERELICT / 07",926,163,1,MUTED);
        }
        if(state.stage==2){
            int x=1110,y=250;int c=hazardActive()?MUTED:DARK;
            g.noFill();g.stroke(c);g.ellipse(x,y,280,280);g.ellipse(x,y,250,250);
            for(int i=0;i<16;i++){float a=i*TWO_PI/16;line(x+cos(a)*43,y+sin(a)*43,x+cos(a)*72,y+sin(a)*72,c);}
            rockSprite(x,y,40,2,888);text("HOLLOW STAR / KEEP CLEAR",x-44,y+85,1,MUTED);
            if(hazardActive())for(int i=-90;i<=90;i+=12)line(x-85,y+i,x+85,y+i,DARK);
            else text("PULSE IN "+(7-(int)(sectorTime%10)),x-18,y-95,1,INK);
        }
    }
    void drawVent(int x,int y){
        int c=hazardActive()?INK:DARK;g.noFill();g.stroke(c);g.ellipse(x,y,130,130);
        border(x-7,y-7,14,14,MUTED);line(x-5,y-5,x+5,y+5,INK);line(x-5,y+5,x+5,y-5,INK);
        if(hazardActive())for(int i=0;i<8;i++){float a=TWO_PI*i/8+sectorTime*.4f;line(x+cos(a)*15,y+sin(a)*15,x+cos(a)*57,y+sin(a)*57,MUTED);}
        text(hazardActive()?"VENTING":"COOLANT VENT",x-22,y+76,1,MUTED);
    }
    void drawStation(){
        int x=(int)baseX,y=(int)baseY;
        for(int i=0;i<4;i++){float a=i*HALF_PI;line(x+cos(a)*27,y+sin(a)*27,x+cos(a)*43,y+sin(a)*43,DARK);}
        border(x-24,y-19,48,38,MUTED);border(x-15,y-15,30,30,INK);box(x-9,y-9,18,18,DARK);
        box(x-3,y-3,6,6,INK);box(x-43,y-12,15,24,DARK);box(x+28,y-12,15,24,DARK);
        for(int i=0;i<3;i++){line(x-41+i*5,y-10,x-41+i*5,y+10,MUTED);line(x+30+i*5,y-10,x+30+i*5,y+10,MUTED);}
        text("ANCHOR",x-11,y+29,1,MUTED);
        if(tick%90<45){box(x-25,y-21,2,2,INK);box(x+24,y+19,2,2,INK);}
    }
    void shipSprite(float x,float y,float angle,boolean thrust){
        g.pushMatrix();g.translate(Math.round(x),Math.round(y));
        // Quantized headings keep the tiny silhouette sharply on the pixel grid.
        float a=Math.round(angle/(PI/8))*(PI/8);float[][] pts={{10,0},{2,-3},{-3,-6},{-6,-6},{-4,-2},{-7,-2},{-7,2},{-4,2},{-6,6},{-3,6},{2,3}};
        g.fill(INK);g.noStroke();g.beginShape();for(float[] p:pts)g.vertex(Math.round(p[0]*cos(a)-p[1]*sin(a)),Math.round(p[0]*sin(a)+p[1]*cos(a)));g.endShape(CLOSE);
        box(Math.round(cos(a)*2)-1,Math.round(sin(a)*2)-1,2,2,BG);
        for(int i=-5;i<=-2;i++)box(Math.round(i*cos(a)),Math.round(i*sin(a)),1,1,MUTED);
        for(int side=-1;side<=1;side+=2){
            box(Math.round(-4*cos(a)-side*4*sin(a)),Math.round(-4*sin(a)+side*4*cos(a)),1,1,BG);
        }
        if(thrust)for(int i=0;i<3;i++){float dx=-9-i*2;box(Math.round(dx*cos(a)),Math.round(dx*sin(a)),i==0?2:1,1,i==2?MUTED:INK);}
        g.popMatrix();
    }
    void rockSprite(float x,float y,int r,int type,int seed){
        Random rng=new Random(seed);g.noStroke();g.fill(type==2?MUTED:DARK);g.beginShape();
        for(int i=0;i<12;i++){float a=TWO_PI*i/12,rr=r*(.75f+rng.nextFloat()*.25f);g.vertex(Math.round(x+cos(a)*rr),Math.round(y+sin(a)*rr));}g.endShape(CLOSE);
        rng.setSeed(seed);float prevX=0,prevY=0,firstX=0,firstY=0;
        for(int i=0;i<12;i++){float a=TWO_PI*i/12,rr=r*(.75f+rng.nextFloat()*.25f),xx=Math.round(x+cos(a)*rr),yy=Math.round(y+sin(a)*rr);if(i>0)line(prevX,prevY,xx,yy,type==2?INK:MUTED);else{firstX=xx;firstY=yy;}prevX=xx;prevY=yy;}line(prevX,prevY,firstX,firstY,type==2?INK:MUTED);
        for(int i=0;i<r/2;i++){int xx=(int)x+rng.nextInt(Math.max(1,r))-r/2,yy=(int)y+rng.nextInt(Math.max(1,r))-r/2;box(xx,yy,2,1,type==2?DARK:MUTED);}
        if(type==1){line(x-4,y+4,x,y-2,INK);line(x,y-2,x+5,y-4,INK);}
        if(type==2){box(x-3,y-3,6,6,BG);border(x-2,y-2,4,4,INK);}
    }
    void bar(int x,int y,int w,int h,float amount,int color){box(x,y,w,h,DARK);box(x,y,Math.round(w*constrain(amount,0,1)),h,color);}
    void drawHud(){
        box(0,0,W,42,BG);line(0,41,W,41,DARK);
        text("VOID / 0"+(state.stage+1),12,10,1,INK);text(SECTORS[state.stage],12,24,1,MUTED);
        text("HULL "+(int)hull+"/"+(int)state.maxHull(),179,9,1,INK);bar(179,22,92,4,hull/state.maxHull(),INK);
        text("CELL "+(int)(energy/state.maxEnergy()*100)+"%",291,9,1,INK);bar(291,22,80,4,energy/state.maxEnergy(),MUTED);
        text("HOLD "+runOre+"/"+state.capacity(),394,9,1,INK);text("CORES "+runCores,394,24,1,MUTED);
        text("BANK "+state.banked[state.stage]+"/"+GameState.GOALS[state.stage],511,9,1,INK);text("CORES "+state.cores[state.stage]+"/"+GameState.CORE_GOALS[state.stage],511,24,1,MUTED);
        box(0,318,W,42,BG);line(0,318,W,318,DARK);text("WASD MOVE / SHIFT BOOST / LMB CUT / E DOCK / ESC PAUSE",12,330,1,MUTED);
        text("CR "+state.credits+"  /  RANGE "+(102+state.upgrades[0]*7),12,347,1,INK);
        // Map: real ore core positions, drones and base; brackets mark the current viewport.
        panel(549,251,79,60);
        for(Rock r:rocks)if(r.hp>0&&r.type==2)box(552+r.x/1400*72,254+r.y/1000*52,2,2,MUTED);
        for(Drone d:drones)if(d.hp>0)box(552+d.x/1400*72,254+d.y/1000*52,1,1,MUTED);
        border(552+baseX/1400*72-2,254+baseY/1000*52-2,4,4,INK);box(552+px/1400*72,254+py/1000*52,2,2,INK);
        text("NAV",604,241,1,MUTED);
        if(state.stage>0){
            text(hazardActive()?"RADIATION / ACTIVE":"RADIATION IN "+(7-(int)(sectorTime%10))+"S",370,347,1,hazardActive()?INK:MUTED);
            if(inHazard(px,py)){panel(206,55,228,19);text(hazardActive()?"HULL EXPOSURE / LEAVE THE RING":"WARNING / RADIATION ZONE",216,62,1,INK);}
        }
    }
    void drawBase(){
        stars(0,0);box(0,0,W,62,BG);text("ANCHOR STATION",20,15,3,INK);text("DRYDOCK / REFIT / SECTOR CONTROL",21,43,1,MUTED);
        text("CREDITS",500,15,1,MUTED);text(String.format("%06d",state.credits),500,29,3,INK);
        line(20,61,620,61,DARK);
        text("0"+(state.stage+1)+" / "+SECTORS[state.stage],20,73,2,INK);
        text(DESCRIPTIONS[state.stage],20,91,1,MUTED);
        text("ORE "+state.banked[state.stage]+" / "+GameState.GOALS[state.stage],385,73,1,INK);
        bar(385,85,103,3,(float)state.banked[state.stage]/GameState.GOALS[state.stage],INK);
        text("CORES "+state.cores[state.stage]+" / "+GameState.CORE_GOALS[state.stage],514,73,1,INK);
        bar(514,85,103,3,(float)state.cores[state.stage]/GameState.CORE_GOALS[state.stage],INK);
        line(20,105,620,105,DARK);text("REFIT ARRAY / 42 NODES",20,115,1,INK);text("CLICK NEXT NODE TO PURCHASE",405,115,1,MUTED);
        hoverBranch=hoverTier=-1;
        for(int b=0;b<GameState.BRANCHES;b++){
            int x=20+b*86;boolean active=b==selected;
            if(over(x,135,80,18)&&click)selected=b;
            text(GameState.BRANCH_NAMES[b],x,136,1,active?INK:MUTED);
            text(String.format("%02d",state.upgrades[b])+" / 06",x,148,1,MUTED);
            line(x+8,170,x+8,261,DARK);
            for(int tier=0;tier<6;tier++){
                int y=166+tier*17;boolean owned=tier<state.upgrades[b],next=tier==state.upgrades[b];
                box(x,y,16,12,owned?INK:BG);border(x,y,16,12,next?INK:DARK);
                text(""+(tier+1),x+6,y+4,1,owned?BG:next?INK:DARK);
                text(tier==5?"FINAL":"MK "+(tier+1),x+23,y+4,1,owned?INK:next?MUTED:DARK);
                if(over(x,y,80,13)){hoverBranch=b;hoverTier=tier;if(click){selected=b;if(next){if(state.buy(b)){save();toast(GameState.NAMES[b][tier]+" INSTALLED");}else toast("INSUFFICIENT CREDITS / NEED "+state.cost(b)+" CR");}else toast(owned?"ALREADY INSTALLED":"INSTALL EARLIER NODES FIRST");}}
            }
        }
        line(20,274,620,274,DARK);
        int b=hoverBranch>=0?hoverBranch:selected,t=hoverTier>=0?hoverTier:Math.min(5,state.upgrades[b]);
        text(GameState.NAMES[b][t],20,285,2,INK);
        String[] effects={"+13 CUT POWER / +7 BEAM RANGE PER NODE","+18 CARGO CAPACITY PER NODE","+14 THRUST / FASTER BOOST PER NODE","+24 ENERGY / +6 RECHARGE PER NODE","+32 MAXIMUM HULL PER NODE","+18 TRACTOR RANGE / +12% ORE VALUE PER NODE","+0.65 HULL REGEN / IMPROVED RESISTANCE PER NODE"};
        text(effects[b],20,304,1,MUTED);
        String price=state.upgrades[b]>=6?"BRANCH COMPLETE":t<state.upgrades[b]?"INSTALLED":"NEXT NODE: "+state.cost(b)+" CR";
        text(price,430,288,1,INK);
        if(button("DEPLOY",20,326,140,23,true))launch();
        if(button(state.stage==2?"FINISH EXPEDITION":"JUMP TO SECTOR 0"+(state.stage+2),173,326,215,23,state.goalMet())){
            if(state.advance()){save();if(state.completed)screen="VICTORY";else{generateWorld();toast("SECTOR UNLOCKED / NEW SIGNAL DETECTED");}}
        }
        if(button("MANUAL",401,326,95,23,true)){previous="BASE";screen="HELP";}
        if(button("MAIN MENU",509,326,110,23,true)){save();screen="TITLE";}
    }
    void drawPause(){
        panel(203,84,234,189);text("SIGNAL PAUSED",225,104,2,INK);
        if(button("RESUME",224,137,192,24,true))screen="FLIGHT";
        if(button("FLIGHT MANUAL",224,172,192,24,true)){previous="PAUSE";screen="HELP";}
        if(button("SETTINGS",224,207,192,24,true)){previous="PAUSE";screen="SETTINGS";}
        text("RETURN TO THE BASE TO SAVE CARGO.",224,252,1,MUTED);
    }
    void drawLost(){
        panel(164,98,312,162);text("SIGNAL LOST",191,118,3,INK);
        text("YOUR UNSOLD CARGO DID NOT MAKE IT.",190,154,1,MUTED);text("THE STATION KEPT YOUR BANKED PROGRESS.",190,169,1,MUTED);
        if(button("RECOVER AT STATION",191,207,258,28,true)){runOre=runCores=0;generateWorld();save();screen="BASE";}
    }
    void drawVictory(){
        stars(now*8,0);center("TRANSMISSION COMPLETE",54,3,INK);center("THE HOLLOW STAR IS QUIET AGAIN.",91,1,MUTED);
        g.pushMatrix();g.translate(320,151);g.scale(4);shipSprite(0,0,-HALF_PI,true);g.popMatrix();
        center("THREE SECTORS. ONE PROSPECTOR.",210,2,INK);center("ORE RECOVERED "+state.totalOre+" / STATION VISITS "+state.trips,239,1,MUTED);
        int n=0;for(int v:state.upgrades)n+=v;center("REFIT NODES "+n+" / 42",255,1,MUTED);
        if(button("RETURN TO STATION / FREE PLAY",185,289,270,28,true))screen="BASE";
        center("END OF DEMO / THANK YOU FOR FLYING",339,1,MUTED);
    }
    public void keyPressed(){
        int k=key==CODED?keyCode:Character.toUpperCase(key);if(k>=0&&k<held.length)held[k]=true;
        if(key==ESC){key=0;if(screen.equals("FLIGHT"))screen="PAUSE";else if(screen.equals("PAUSE"))screen="FLIGHT";else if(screen.equals("HELP")||screen.equals("SETTINGS"))screen=previous;}
        if(screen.equals("CONFIRM")){
            if(k=='Y'){state=new GameState();generateWorld();save();screen="BASE";}
            if(k=='N')screen="TITLE";
        }
    }
    public void keyReleased(){int k=key==CODED?keyCode:Character.toUpperCase(key);if(k>=0&&k<held.length)held[k]=false;}
    public void mousePressed(){if(mouseButton==LEFT)click=true;}
    public void focusLost(){Arrays.fill(held,false);mousePressed=false;if(screen.equals("FLIGHT"))screen="PAUSE";}
    class Rock{float x,y,hp,maxHp;int radius,type,seed;Rock(float x,float y,int type,int seed){this.x=x;this.y=y;this.type=type;this.seed=seed;radius=type==2?23:type==1?17:12;maxHp=(type==2?105:type==1?62:32)*(1+state.stage*.35f);hp=maxHp;}}
    class Drone{float x,y,hp,maxHp,phase,cooldown;Drone(float x,float y){this.x=x;this.y=y;maxHp=hp=40+state.stage*25;cooldown=2;}}
    class Drop{float x,y,vx,vy,age;boolean core;Drop(float x,float y,boolean core){this.x=x;this.y=y;this.core=core;vx=random(-18,18);vy=random(-18,18);}}
    class Particle{float x,y,vx,vy,life;Particle(float x,float y){this.x=x;this.y=y;vx=random(-45,45);vy=random(-45,45);life=random(.15f,.65f);}}
    class Bolt{float x,y,vx,vy,life=5;Bolt(float x,float y,float a){this.x=x;this.y=y;vx=cos(a)*(90+state.stage*15);vy=sin(a)*(90+state.stage*15);}}
    void smokeSequence(){
        // Runs the actual renderer and gameplay in a virtual display; never touches a user's save.
        if(tick==3){g.save("build/title.png");saveFrame("build/window.png");screen="BASE";}
        if(tick==5){g.save("build/station.png");launch();}
        if(tick==8){g.save("build/flight.png");screen="HELP";}
        if(tick==10){g.save("build/manual.png");screen="VICTORY";}
        if(tick==12){g.save("build/victory.png");screen="SETTINGS";}
        if(tick==14){g.save("build/settings.png");screen="CONFIRM";}
        if(tick==16){g.save("build/confirm.png");screen="PAUSE";}
        if(tick==18){g.save("build/pause.png");screen="LOST";}
        if(tick==20){g.save("build/recovery.png");state.stage=1;generateWorld();screen="FLIGHT";}
        if(tick==22){g.save("build/sector2.png");state.stage=2;generateWorld();screen="FLIGHT";}
        if(tick==24){g.save("build/sector3.png");System.out.println("RENDER_SMOKE_OK: all menus, flight, three sectors, recovery, victory");exit();}
    }
}
