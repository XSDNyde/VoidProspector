import java.nio.file.*;
import java.util.*;
import processing.core.PConstants;

/** Run with ./test.sh; exercises production progression, save IO and flight simulation. */
public final class GameTests {
    static int checks;
    static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        Path temp=Files.createTempDirectory("void-tests-");
        try {
            progressionAndSave(temp);
            flightAndCombat(temp);
            campaign(temp);
            check(PixelFont.width("VOID",2)==30,"bitmap text metrics");
            System.out.println("PASS: "+checks+" checks / upgrades, save roundtrip, mining, cargo, combat, all 3 stages");
        } finally {
            try(var paths=Files.walk(temp)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}
        }
    }
    static void progressionAndSave(Path dir)throws Exception{
        GameState s=new GameState();
        check(!s.advance(),"cannot jump without ore and cores");
        check(!s.buy(0),"cannot purchase without funds");
        check(!s.buy(-1)&&!s.buy(7),"invalid upgrade branch rejected");
        s.credits=100000;
        for(int b=0;b<7;b++)for(int tier=0;tier<6;tier++){
            int cost=s.cost(b),before=s.credits;
            check(s.buy(b),"can buy node "+b+"/"+tier);
            check(s.credits==before-cost&&s.upgrades[b]==tier+1,"node cost and level applied");
        }
        check(!s.buy(0),"upgrade cap enforced");
        check(s.damage()>28&&s.capacity()>35&&s.speed()>77&&s.maxEnergy()>85&&s.maxHull()>100&&s.magnet()>36&&s.regen()>0&&s.resistance()<1,"all seven branches change gameplay");
        s.deposit(70,0);check(!s.goalMet(),"cores required separately");
        s.deposit(0,1);check(s.advance()&&s.stage==1,"first stage jump");
        s.deposit(160,2);check(s.advance()&&s.stage==2,"second stage jump");
        s.deposit(280,3);check(s.advance()&&s.completed,"third stage completes demo");
        Path file=dir.resolve("roundtrip.properties");s.save(file);GameState loaded=GameState.load(file);
        check(loaded.completed&&loaded.stage==2&&loaded.unlocked==3,"campaign persists");
        check(loaded.credits==s.credits&&Arrays.equals(loaded.upgrades,s.upgrades)&&Arrays.equals(loaded.cores,s.cores),"economy persists");
        s.save(file);check(GameState.load(file).credits==s.credits,"repeat save atomic replacement");
        Files.writeString(file,"version=900\n");boolean rejected=false;
        try{GameState.load(file);}catch(java.io.IOException e){rejected=true;}check(rejected,"unknown save format rejected");
        Files.writeString(file,"version=1\ncredits=-1\n");rejected=false;
        try{GameState.load(file);}catch(java.io.IOException e){rejected=true;}check(rejected,"corrupt save rejected");
    }
    static VoidProspector game(Path dir){
        VoidProspector g=new VoidProspector();g.savePath=dir.resolve("simulation.properties");g.dt=1f/60;g.screen="FLIGHT";g.particlesOn=false;g.generateWorld();return g;
    }
    static void step(VoidProspector g,int n){for(int i=0;i<n;i++)g.updateFlight();}
    static void flightAndCombat(Path dir){
        VoidProspector g=game(dir);check(g.rocks.size()==92&&g.drones.size()==3,"stage one world and enemies generated");
        for(VoidProspector.Rock r:g.rocks)check(VoidProspector.dist(r.x,r.y,g.baseX,g.baseY)>=120,"station safety radius");
        g.rocks.clear();g.drones.clear();g.invulnerable=0;
        g.px=300;g.py=300;g.camX=g.px-320;g.camY=g.py-180;g.mx=420;g.my=180;
        g.held['D']=true;step(g,60);check(g.px>350,"thrust moves ship");g.held['D']=false;
        g.energy=30;g.mousePressed=false;step(g,60);check(g.energy>45,"reactor recharges");
        g.px=500;g.py=300;g.vx=g.vy=0;g.hull=100;g.invulnerable=0;
        VoidProspector.Bolt bolt=g.new Bolt(g.px,g.py,0);bolt.vx=bolt.vy=0;g.bolts.add(bolt);step(g,1);
        check(g.hull<100&&g.bolts.isEmpty(),"enemy projectile damages hull and expires");
        float hull=g.hull;g.damage(10);check(g.hull==hull,"hit invulnerability prevents stacked hits");
        g.invulnerable=0;g.damage(1000);step(g,1);check(g.screen.equals("LOST"),"hull depletion triggers recovery screen");
        g=game(dir);g.rocks.clear();g.drones.clear();g.px=400;g.py=300;g.invulnerable=10;
        VoidProspector.Drone enemy=g.new Drone(580,300);enemy.cooldown=0;g.drones.add(enemy);step(g,1);
        check(g.shotsFired==1&&!g.bolts.isEmpty(),"active drones fire at player");
        g=game(dir);g.rocks.clear();g.drones.clear();g.state.stage=1;g.px=375;g.py=290;g.sectorTime=8;g.invulnerable=0;
        float beforeHazard=g.hull;step(g,1);check(g.hull<beforeHazard,"second sector coolant vents damage during active cycle");
        g.invulnerable=0;g.sectorTime=1;beforeHazard=g.hull;step(g,1);check(g.hull==beforeHazard,"vent safe during cooldown");
        g.state.stage=2;g.px=1110;g.py=250;g.sectorTime=8;g.invulnerable=0;step(g,1);check(g.hull<beforeHazard,"third sector star radiation damages");
        g=game(dir);g.rocks.clear();g.drones.clear();g.runOre=g.state.capacity();
        g.drops.add(g.new Drop(g.px,g.py,false));g.drops.add(g.new Drop(g.px,g.py,true));step(g,1);
        check(g.runOre==g.state.capacity()&&g.runCores==1&&g.drops.size()==1,"cargo cap preserves ore but still accepts core");
        g.held['E']=true;g.px=g.baseX;g.py=g.baseY;step(g,1);
        check(g.screen.equals("BASE")&&g.runOre==0&&g.state.banked[0]==g.state.capacity(),"E docks and banks cargo");
        check(g.hull==g.state.maxHull()&&g.energy==g.state.maxEnergy()&&Files.exists(g.savePath),"station repairs recharges and saves");
    }
    static void campaign(Path dir){
        VoidProspector g=game(dir);
        for(int sector=0;sector<3;sector++){
            check(g.state.stage==sector,"campaign sector index");
            check(g.rocks.size()==92+sector*28&&g.drones.size()==3+sector*3,"sector danger increases");
            long coreRocks=g.rocks.stream().filter(r->r.type==2).count();check(coreRocks>=GameState.CORE_GOALS[sector],"enough core deposits");
            // Controlled mining scene isolates extraction from the separately tested combat.
            while(!g.state.goalMet()){
                g.rocks.clear();g.drones.clear();g.drops.clear();g.bolts.clear();g.screen="FLIGHT";
                g.px=300;g.py=300;g.vx=g.vy=0;g.camX=-20;g.camY=120;g.mx=390;g.my=180;
                g.hull=g.state.maxHull();g.energy=g.state.maxEnergy();g.invulnerable=10;
                VoidProspector.Rock rock=g.new Rock(360,300,2,93);g.rocks.add(rock);
                g.mouseButton=PConstants.LEFT;
                for(int i=0;i<900&&rock.hp>0;i++){
                    g.mousePressed=g.energy>2;g.updateFlight();
                    if(g.energy<2){g.mousePressed=false;step(g,90);}
                }
                check(rock.hp<=0,"beam breaks sector "+sector+" core asteroid");
                check(g.drops.stream().anyMatch(d->d.core),"core released by mining");
                g.mousePressed=false;g.px=360;g.py=300;step(g,120);
                check(g.runOre>0&&g.runCores>0,"mined ore and core enter cargo");
                int ore=g.runOre,credits=g.state.credits;g.dock();
                check(g.state.credits>credits&&g.runOre==0,"sale earns credits");
                check(g.state.banked[sector]>=ore,"deposit counts toward sector objective");
            }
            check(g.state.advance(),"jump from sector "+sector);
            g.save();check(Files.exists(g.savePath),"campaign autosaved");
            if(sector<2)g.generateWorld();
        }
        check(g.state.completed&&g.state.unlocked==3,"full campaign reaches ending");
    }
}
