package com.Ezzuneware.EzScurrius;

public class ezProjectile {
    public final int projectileId;
    public final ezProjectileType projectileType;
    public int projectileTimer;

    ezProjectile(int projectileId, ezProjectileType projectileType, int projectileTimer){
        this.projectileId = projectileId;
        this.projectileType = projectileType;
        this.projectileTimer = projectileTimer;
    }

}
