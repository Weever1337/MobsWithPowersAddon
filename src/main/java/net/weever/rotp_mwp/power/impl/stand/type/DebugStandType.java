package net.weever.rotp_mwp.power.impl.stand.type;

import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.power.impl.stand.stats.StandStats;

import com.github.standobyte.jojo.power.impl.stand.type.StandType;
import net.minecraft.entity.LivingEntity;

public class DebugStandType<T extends StandStats> extends StandType<T> {
    protected DebugStandType(Builder<T> builder) {
        super(builder);
    }

    @Override
    public boolean summon(LivingEntity user, IStandPower standPower, boolean withoutNameVoiceLine) {
        return false;
    }

    @Override
    public void unsummon(LivingEntity user, IStandPower standPower) {}

    @Override
    public void forceUnsummon(LivingEntity user, IStandPower standPower) {}

    public static class Builder<T extends StandStats> extends StandType.AbstractBuilder<Builder<T>, T>{

        @Override
        protected Builder<T> getThis() {
            return this;
        }

        @Override
        public DebugStandType<T> build() {
            return new DebugStandType<>(this);
        }
    }


}
