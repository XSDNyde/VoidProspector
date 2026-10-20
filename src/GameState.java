import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Persistent progression and balancing, independent of the renderer. */
public final class GameState {
    public static final int BRANCHES=7, DEPTH=6;
    public static final String[] BRANCH_NAMES={"EXTRACTION","HOLD","PROPULSION","REACTOR","ARMOR","SALVAGE","DEFENSE"};
    public static final String[][] NAMES={
        {"CUTTER I","FOCUS LENS","HOT PLASMA","TWIN COIL","DEEP CUT","STAR DRILL"},
        {"RACK I","ORE CRADLE","BULKHEAD","FOLD HOLD","DEEP VAULT","VOID BARGE"},
        {"THRUST I","ION NOZZLE","VECTOR JET","AFTERBURN","SLIP DRIVE","STAR RUNNER"},
        {"CELL I","CAPACITOR","FLUX LOOP","FAST CHARGE","FUSION CELL","MICRO SUN"},
        {"PLATE I","BRACE","HULL WEAVE","NANO SHELL","TITAN SKIN","MONOLITH"},
        {"TRACTOR I","LONG REACH","ORE SORTER","RICH YIELD","AUTO SWEEP","MIDAS ARRAY"},
        {"REPAIR I","REGEN MESH","FIELD COIL","ABLATIVE","AEGIS","IMMORTAL"}
    };
    public static final int[] GOALS={70,160,280}, CORE_GOALS={1,2,3};
    public int credits, stage, unlocked=1, totalOre, trips;
    public boolean completed;
    public final int[] upgrades=new int[BRANCHES], banked=new int[3], cores=new int[3];
    public int cost(int branch) { return 12+upgrades[branch]*18+upgrades[branch]*upgrades[branch]*5; }
    public boolean buy(int branch) {
        if(branch<0||branch>=BRANCHES||upgrades[branch]>=DEPTH||credits<cost(branch))return false;
        credits-=cost(branch); upgrades[branch]++; return true;
    }
    public int capacity(){return 35+upgrades[1]*18;}
    public float speed(){return 77+upgrades[2]*14;}
    public float damage(){return 28+upgrades[0]*13;}
    public float maxEnergy(){return 85+upgrades[3]*24;}
    public float recharge(){return 19+upgrades[3]*6;}
    public float maxHull(){return 100+upgrades[4]*32;}
    public float magnet(){return 36+upgrades[5]*18;}
    public float regen(){return upgrades[6]*0.65f;}
    public float resistance(){return 1f/(1f+upgrades[6]*0.13f);}
    public int value(int ore){return Math.round(ore*(1+stage*.45f+upgrades[5]*.12f));}
    public void deposit(int ore,int foundCores){
        banked[stage]+=ore; cores[stage]+=foundCores; totalOre+=ore;
        credits+=value(ore)+foundCores*(20+stage*15); trips++;
    }
    public boolean goalMet(){return banked[stage]>=GOALS[stage]&&cores[stage]>=CORE_GOALS[stage];}
    public boolean advance(){
        if(!goalMet())return false;
        if(stage==2){completed=true;return true;}
        stage++; unlocked=Math.max(unlocked,stage+1);return true;
    }
    public void save(Path file) throws IOException {
        Properties p=new Properties();p.setProperty("version","1");
        p.setProperty("credits",""+credits);p.setProperty("stage",""+stage);
        p.setProperty("unlocked",""+unlocked);p.setProperty("totalOre",""+totalOre);
        p.setProperty("trips",""+trips);p.setProperty("completed",""+completed);
        for(int i=0;i<BRANCHES;i++)p.setProperty("upgrade."+i,""+upgrades[i]);
        for(int i=0;i<3;i++){p.setProperty("ore."+i,""+banked[i]);p.setProperty("core."+i,""+cores[i]);}
        Files.createDirectories(file.toAbsolutePath().getParent());
        Path temp=file.resolveSibling(file.getFileName()+".tmp");
        try(OutputStream out=Files.newOutputStream(temp)){p.store(out,"Void Prospector - station autosave");}
        try{Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
        catch(AtomicMoveNotSupportedException e){Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);}
    }
    public static GameState load(Path file) throws IOException {
        Properties p=new Properties();try(InputStream in=Files.newInputStream(file)){p.load(in);}
        if(!"1".equals(p.getProperty("version")))throw new IOException("Unsupported save version");
        GameState s=new GameState();
        try{
            s.credits=number(p,"credits",0,10000000);s.stage=number(p,"stage",0,2);
            s.unlocked=number(p,"unlocked",1,3);s.totalOre=number(p,"totalOre",0,10000000);
            s.trips=number(p,"trips",0,10000000);s.completed=Boolean.parseBoolean(p.getProperty("completed"));
            for(int i=0;i<BRANCHES;i++)s.upgrades[i]=number(p,"upgrade."+i,0,DEPTH);
            for(int i=0;i<3;i++){s.banked[i]=number(p,"ore."+i,0,10000000);s.cores[i]=number(p,"core."+i,0,10000000);}
        }catch(RuntimeException e){throw new IOException("Invalid save data",e);}
        if(s.unlocked<s.stage+1)throw new IOException("Invalid stage progression");
        return s;
    }
    private static int number(Properties p,String key,int min,int max){
        int v=Integer.parseInt(p.getProperty(key));if(v<min||v>max)throw new IllegalArgumentException(key);return v;
    }
}
