/**
 * This class was created by <Vazkii>. It's distributed as
 * part of the Psi Mod. Get the Source Code in github:
 * https://github.com/Vazkii/Psi
 * 
 * Psi is Open Source and distributed under the
 * CC-BY-NC-SA 3.0 License: https://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB
 * 
 * File Created @ [24/01/2016, 15:35:27 (GMT)]
 */
package vazkii.psi.common.spell.trick.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import vazkii.psi.api.internal.Vector3;
import vazkii.psi.api.spell.EnumSpellStat;
import vazkii.psi.api.spell.Spell;
import vazkii.psi.api.spell.SpellCompilationException;
import vazkii.psi.api.spell.SpellContext;
import vazkii.psi.api.spell.SpellMetadata;
import vazkii.psi.api.spell.SpellParam;
import vazkii.psi.api.spell.SpellRuntimeException;
import vazkii.psi.api.spell.param.ParamVector;
import vazkii.psi.api.spell.piece.PieceTrick;
import vazkii.psi.common.core.handler.ConfigHandler;

public class PieceTrickBreakBlock extends PieceTrick {

	SpellParam position;

	public PieceTrickBreakBlock(Spell spell) {
		super(spell);
	}

	@Override
	public void initParams() {
		addParam(position = new ParamVector(SpellParam.GENERIC_NAME_POSITION, SpellParam.BLUE, false, false));
	}

	@Override
	public void addToMetadata(SpellMetadata meta) throws SpellCompilationException {
		super.addToMetadata(meta);

		meta.addStat(EnumSpellStat.POTENCY, 20);
		meta.addStat(EnumSpellStat.COST, 40);
	}

	@Override
	public Object execute(SpellContext context) throws SpellRuntimeException {
		if(context.caster.worldObj.isRemote)
			return null;

		Vector3 positionVal = this.<Vector3>getParamValue(context, position);
		
		if(positionVal == null)
			throw new SpellRuntimeException(SpellRuntimeException.NULL_VECTOR);
		if(!context.isInRadius(positionVal))
			throw new SpellRuntimeException(SpellRuntimeException.OUTSIDE_RADIUS);

		BlockPos pos = new BlockPos(positionVal.x, positionVal.y, positionVal.z);
		removeBlockWithDrops(context.caster, context.caster.worldObj, pos, ConfigHandler.cadHarvestLevel, false, 0, true);

		return null;
	}

	public static void removeBlockWithDrops(EntityPlayer player, World world, BlockPos pos, int harvestLevel, boolean silk, int fortune, boolean particles) {
		if(!world.isBlockLoaded(pos))
			return;
		
		IBlockState state = world.getBlockState(pos);
		Block block = state.getBlock();
		if(!world.isRemote && block != null && !block.isAir(world, pos) && block.getPlayerRelativeBlockHardness(player, world, pos) > 0) {
			int neededHarvestLevel = block.getHarvestLevel(state);
			if(neededHarvestLevel > harvestLevel)
				return;

			BreakEvent event = new BreakEvent(world, pos, state, player);
			MinecraftForge.EVENT_BUS.post(event);
			if(!event.isCanceled()) {
				if(!player.capabilities.isCreativeMode) {
					block.onBlockHarvested(world, pos, state, player);

					if(block.removedByPlayer(world, pos, player, true)) {
						block.onBlockDestroyedByPlayer(world, pos, state);
						block.harvestBlock(world, player, pos, state, world.getTileEntity(pos));
					}
				} else world.setBlockToAir(pos);	
			}

			if(particles)
				world.playAuxSFX(2001, pos, Block.getStateId(state));
		}
	}
}