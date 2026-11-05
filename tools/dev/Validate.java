import ss.core.*; import ss.game.*; import ss.level.*;
public class Validate {
  public static void main(String[] a) throws Exception {
    Game.initAssets();
    for (int id = 0; id < 2; id++) {
      Game g = new Game();
      g.loadLevel(id);
      Level lv = g.level;
      System.out.println("== " + lv.name + " zone x " + lv.zoneX0 + ".." + lv.zoneX1
          + "  z " + lv.zoneZ0 + ".." + lv.zoneZ1);
      int inZone = 0, outZone = 0;
      for (int i = 0; i < Band.ntrig; i++) {
        int o = i * 7;
        int x0 = Band.trigData[o], z0 = Band.trigData[o+1];
        int x1 = Band.trigData[o+2], z1 = Band.trigData[o+3];
        boolean in = x0 >= lv.zoneX0 && x1 <= lv.zoneX1 && z0 >= lv.zoneZ0 && z1 <= lv.zoneZ1;
        if (in) inZone++; else {
          outZone++;
          System.out.println("  trig " + i + " kind=" + Band.trigData[o+4] + " at " + x0 + "," + z0
              + ".." + x1 + "," + z1 + " OUTSIDE zone");
        }
      }
      System.out.println("  triggers in zone=" + inZone + " out=" + outZone);
      // spawn must be walkable
      g.player.x = lv.spawnX; g.player.z = lv.spawnZ;
      System.out.println("  spawn " + lv.spawnX + "," + lv.spawnZ
          + " blocked=" + g.isBlocked(lv.spawnX, lv.spawnZ, 20));
      int its = 0, itsOut = 0;
      for (int i = 0; i < lv.nbands; i++) {
        Band b = lv.bands[i]; if (b == null) continue;
        for (int k = 0; k < b.ni; k++) {
          its++;
          int x = b.it[k*5+1], z = b.it[k*5+3];
          if (x < lv.zoneX0 || x > lv.zoneX1 || z < lv.zoneZ0 || z > lv.zoneZ1) {
            itsOut++; System.out.println("  item " + k + " at " + x + "," + z + " outside zone");
          } else if (g.isBlocked(x, z, 16)) {
            System.out.println("  item " + k + " at " + x + "," + z + " is inside a collider");
          }
        }
      }
      System.out.println("  items=" + its + " outOfZone=" + itsOut);
    }
  }
}
