package de.cubeisland.cubeengine.basics.command.moderation;

import de.cubeisland.cubeengine.basics.Basics;
import de.cubeisland.cubeengine.core.command.CommandContext;
import de.cubeisland.cubeengine.core.command.reflected.Command;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.material.Openable;

public class DoorCommand
{
    private Basics basics;
    
    public DoorCommand( Basics basics )
    {
        this.basics = basics;
    }

    @Command
    (
            desc = "",
            usage = "<open|close> <radius> <world> <x> <y> <z>",
            min = 2,
            max = 6 
    )
    public void doors( CommandContext context )
    {
        context.sendMessage( "command is not implemented yet.");
        return;
        
//        //TODO flags and permissions for all types of doors
//        User user = ( User ) context.getSender();
//        boolean open = context.getArg( 0, String.class).equalsIgnoreCase( "open" );
//        int radius = context.getArg( 1, Integer.class, 10 );
//
//        // TODO: find all blocks in the radius. Does not work accurately. USE Shapes!
//        Vector userVector = user.getLocation().toVector();      // TODO use specified location.
//        Set<BlockVector3> blockVectors = new HashSet<BlockVector3>();
//
//        for( int circle = radius; circle > 0; circle-- )
//        {
//            double stepSizeLatitudes = 180.0 / (circle * 10);
//            double stepSizeLongitudes = 360.0 / (circle * 10);
//
//            for( int width = 0; width < circle * 10; width++ )
//            {
//                double widthAngle = (90.0 - (width + 1) * 180.0 / stepSizeLatitudes) * Math.PI / 180.0;
//
//                for( int length = 0; length < circle * 10; length++ )
//                {
//                    double lengthAngle = length * 360 / stepSizeLongitudes * Math.PI / 180.0;
//
//                    blockVectors.add( new BlockVector3(
//                            ( int ) (Math.cos( lengthAngle ) * Math.cos( widthAngle ) * circle + userVector.getX()),
//                            ( int ) (Math.sin( widthAngle ) * circle + userVector.getY()),
//                            ( int ) (Math.sin( lengthAngle ) * Math.cos( widthAngle ) * circle + userVector.getZ()) ) );
//                }
//            }
//        }
//
//        for( BlockVector3 vector : blockVectors )
//        {
//            Block block = user.getWorld().getBlockAt( vector.x, vector.y, vector.z );
//            this.setOpen( block, open );
//        }
//        
//        if(open)
//        {
//            context.sendMessage( "basics", "&aAll doors are open now" );
//        }
//        else
//        {
//            context.sendMessage( "basics", "&aAll doors are closed now" );
//        }
    }

    /**
     * sets the block either open or closed.
     * @param block - the block
     * @param open - true to set open, false to set closed
     * @return returns whether the block could set or not
     */
    public boolean setOpen( Block block, boolean open )
    {
        Material type = block.getType();
        BlockState state = block.getState();
        
        if( !this.isOpenable( state ) )
        {
            return false;
        }
        
        byte rawData = state.getRawData();
        
        if( (type == Material.WOODEN_DOOR || type == Material.IRON_DOOR_BLOCK) && (rawData & 0x8) == 0x8 )
        {
            return false;
        }

        if( open )
        {
            state.setRawData( ( byte ) (rawData | 0x4) );   // open door
        }
        else
        {
            state.setRawData( ( byte ) (rawData & 0xB) );    //close door
        }
        state.update();
        return true;
    }
    
    /**
     * This method checks whether a Block is openable. 
     * @param state - The state of the block.
     * @return whether the meterialdate of the block implements openable or not.
     */
    public boolean isOpenable(BlockState state)
    {
        Class<?>[] interfaces = state.getData().getClass().getInterfaces();
        for(int i = 0; i < interfaces.length; i++)
        {
            if(interfaces[i] == Openable.class)
            {
                return true;
            }     
        }
        return false;
    }
}
