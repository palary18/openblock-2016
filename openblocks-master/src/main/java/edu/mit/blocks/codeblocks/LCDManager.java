package edu.mit.blocks.codeblocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.mit.blocks.renderable.RenderableBlock;

import edu.mit.blocks.codeblocks.Block;
import edu.mit.blocks.codeblocks.BlockConnector;
import edu.mit.blocks.codeblocks.BlockLink;
import edu.mit.blocks.codeblocks.BlockStub;
import edu.mit.blocks.codeblocks.BlockConnector.PositionType;
import edu.mit.blocks.codeblocks.SLBlockProperties.RuntimeType;
//import edu.mit.blocks.codeblocks.SLBlockProperties;

import edu.mit.blocks.workspace.Workspace;
import edu.mit.blocks.workspace.WorkspaceEvent;
import edu.mit.blocks.workspace.WorkspaceWidget;
import edu.mit.blocks.controller.WorkspaceController;
import edu.mit.blocks.workspace.WorkspaceEnvironment;

//import com.ardublock.translator.Translator;

/**
 * This class manages all procedure to output type mappings. This class
 * makes best-effort attempts to synchronize/update output block sockets.
 * This class also tries to update procedure callers when such things happen.
 * 
 * <p>Note that even with this class, we still need to implement separate 
 * procedure output checking in the compiler, since we make no guarantees 
 * about code branches or even output types (since we don't modify blocks
 * that are already connected to something). 
 */
public class LCDManager
{   
	private static Workspace workspace;
	
    /** Default starting space for output blocks. */
    private final static int DEFAULT_SIZE = 5;
    
    /**
     * We use this class to encapsulate the procedure output information.
     * This includes the procedure block, all output blocks contained in it,
     * and whether it has a type.
     */
    private static class VariableInfo {
        private final List<Long> variables = new ArrayList<Long>(DEFAULT_SIZE);
        private String type = null;
        
        /** The number of output blocks that are connected to something. */
        private int numTyped = 0;
        private boolean initValue = false;
    }
    
    /** The procedure block to output type mapping. */
    private final static Map<Long, VariableInfo> myVarInfo = 
        new HashMap<Long, VariableInfo>();
    
    /** 
     * Constructs a ProcedureOutputManager instance, which is required to deal with
     * adding and removing variables from a procedure by keeping track of myVarInfo.
     * @param wworkspace: the Workspace instance to manage the Procedure variables.
     * 	(needs to be static because we call it in some static methods)
     */
    public LCDManager(Workspace wworkspace) {
    	workspace = wworkspace;
    }
    
    /** Value corresponds to the vm command name its corresponding Block */
    public final static String VM_COMMAND_NAME = "vm-cmd-name";
    public final static String GLOBAL_VARIABLE = "global-variable";
    
    public final static String CMD_EVAL_PROCEDURE = "eval-procedure";

    public final static String RUNTIME_TYPE = "runtime-type";
    
    // Event Management =========================
    // MUST BE CALLED AFTER POLYRULE
    /**
     * Similar to a WorkspaceListener, but not an actual listener because it is
     * never added to the list of listeners and can only be called after PolyRule.
     * This is because output is a poly block that needs to be handled before
     * attaching an output to a procedure can occur (if the output type is not
     * dealt with first, we can't use it to determine the procedure type).
     * 
     * @param event: WorkspaceEvent holds the information about the block that is
     * being dealt with (BlockID, Link, Widget, etc.)
     * TODO: Maybe we can abstract this even more from listener by only passing info
     * necessary for the update instead of a whole event, but the issue is that
     * I think the event is necessary to call the other listeners in the case that
     * variables don't match and we have to disconnect blocks...
     */
    public static void LCDUpdateInfo(WorkspaceEvent event) {
        Block b = getBlock(event.getSourceBlockID());
        BlockLink link = event.getSourceLink();
        
        switch (event.getEventType()) {
        case WorkspaceEvent.BLOCKS_CONNECTED:
            if (link != null) {
                blocksConnected(event.getSourceWidget(),
                                link.getSocketBlockID(), 
                                link.getPlugBlockID(),
                                link.getSocket());
            }
            return;
            
        case WorkspaceEvent.BLOCKS_DISCONNECTED:
            if (link != null) {
                blocksDisconnected(event.getSourceWidget(),
                                   link.getSocketBlockID(), 
                                   link.getPlugBlockID());
            }
            return;
            
        case WorkspaceEvent.BLOCK_ADDED:
            if (b != null) {
                if (b.isVariableDeclBlock()) {
                    // Create a new entry for this variable 
                    myVarInfo.put(b.getBlockID(), new VariableInfo());
                }
            }
            return;
            
        case WorkspaceEvent.BLOCK_REMOVED:
            if (b != null && b.isVariableDeclBlock()) {
            	// System.out.println("procedure of type "+myVarInfo.get(b.getBlockID()).type+" removed.");
                // Remove our entry.
                myVarInfo.remove(b.getBlockID());
                if (link != null) {
                	blocksDisconnected(event.getSourceWidget(),
                			link.getSocketBlockID(),
                			link.getPlugBlockID());
                }
            }
            return;
        }
    }
    
    /**
     * After loading canvas, does a final sweep of all procedures to update
     * myVarInfo. It does this by going through each output block and seeing
     * if that output contributes any information to a procedure.
     * Then it updates the procedure's stubs if the procedure type changes.
     * 
     * Called after regular loading is finished (WorkspaceController.LoadSaveString()).
     * 
     * Updating myVarInfo depends on one crucial assumption:
     * The types of all variables should be consistent because they were handled
     * prior to the original saving with the POM (so the loaded version is correct).
	 * Therefore, we DO NOT need to:
	 * 		change the types of empty output sockets
	 *		revert the types of incorrect output sockets
	 *		disconnect incorrect output types
     */
    public static void finishLoad() {
    	
    	// Create new output info for each procedure on the canvas.
    	for(Block p : workspace.getBlocksFromGenus("variable")) {
    		myVarInfo.put(p.getBlockID(), new VariableInfo());
    	}
    	
    	// Update myVarInfo by checking each output.
    	for(Block b : workspace.getBlocksFromGenus("getterglobal-var")) {
    		// Only handle the output blocks that are connected to a procedure.
    		Long decVar = getDecVarBlockID(b.getBlockID());    		
    		if (decVar != null && workspace.getEnv().getBlock(decVar).isVariableDeclBlock()) {
    			//System.out.println("procedure (decVar) number "+decVar);
    			VariableInfo info = myVarInfo.get(decVar);
    			
    			// Check that the output hasn't already been visited (shouldn't have but just to be safe...)
				if (!info.variables.contains(b)) {
					
					// Change the procedure type if it has not been set and is not of the generic type poly.
    				if (info.type == null && !b.getSocketAt(0).getKind().equals("poly")) {
    					info.type = b.getSocketAt(0).getKind();
    					
    					// Update stubs due to change in type
    					BlockStub.parentPlugChanged(workspace, decVar, info.type);
    				}
    				
    				// Increase the number of typed variables (aka connected variables without empty sockets).
    				if (b.getSocketAt(0).getBlockID() != -1) {
    					info.numTyped++;
    				}
    				
    				// Add all variables that have not already been added (regardless of type or empty/nonempty sockets).
   					info.variables.add(b.getBlockID());
   					
    			}
    			//System.out.println("numtyped "+info.numTyped+" type "+info.type);
    		}
    	}
    }
    
    /**
     * Clears the information in myVarInfo. This should only be called
     * when resetting the workspace (WorkspaceController.resetWorkspace()), 
     * which occurs when loading a project or creating a new one.
     */
    public static void reset() {
    	myVarInfo.clear();
    }
    
    /**
     * When blocks are connected, check to see whether we are in a proc
     * stack. If so, check to see whether we should update the output type,
     * and if any existing blocks are affected by this change. 
     */
    private static void blocksConnected(WorkspaceWidget w, Long socket, Long plug, BlockConnector sock) {
        // Don't do anything if we're not in a variable setter.
        Long decVar = getDecVarBlockID(plug);
        
        boolean add = false;
        Block bs = workspace.getEnv().getBlock(socket);
        boolean socketDecVar = workspace.getEnv().getBlock(socket).isVariableDeclBlock();
        
        if (socketDecVar) {
        	VariableInfo info = myVarInfo.get(workspace.getEnv().getBlock(socket).getBlockID());
            List<WorkspaceEvent> events = new ArrayList<WorkspaceEvent>();
        	//examineType(add, b, info, sock);
                       
            info.initValue = true;
            info.numTyped++;
            info.type = workspace.getEnv().getBlock(plug).getPlug().getKind();

            if (info.type != null) {
                changeType(info, w, events);
                BlockStub.parentVarPlugChanged(workspace, bs.getBlockID(), info.type);
            }
        	return;
        }
//System.out.println(b.getBlockLabel());
        if (decVar == null || !workspace.getEnv().getBlock(decVar).isVariableDeclBlock()) return;

        // If the proc stack already has a type, change all variables in the
        // new part to that type and disconnect anything that doesn't fit.

        
        // If the variable was added to a setter block,
        // then b is the setter block (socket = output) and add is true.
        // else b is the current block (plug) and add is false.
        // in changeType, we check if b is type setter before proceeding.
        
        add = true;
        Block b = workspace.getEnv().getBlock(plug);
        VariableInfo info = myVarInfo.get(decVar);
        List<WorkspaceEvent> events = new ArrayList<WorkspaceEvent>();
    	
        if (info.type != null)
            changeType(add, b, info.type, info, w, events);
        else {
            // Examine the type. If there is a type in the new portion of
            // the stack, reset all the previous output blocks to that type.
            examineType(add, b, info, sock);
            if (info.type != null) {
                changeType(info, w, events);
                BlockStub.parentVarPlugChanged(workspace, decVar, info.type);
                Block bp = workspace.getEnv().getBlock(decVar);
                bp.setSocketAt(0, info.type, PositionType.SINGLE, "", false, false, Block.NULL);
                workspace.getEnv().getRenderableBlock(decVar).updateConnectors();
                bp.notifyRenderable();
            }
        }
        
        // Fire events.
        if (!events.isEmpty()) {
            //WorkspaceController.getInstance().notifyListeners(events);
            for (WorkspaceEvent e : events) {
                workspace.notifyListeners(e);
            }
        }
        	
    }
    
    /**
     * When blocks are disconnected, update the proc stack and revert any
     * affected output blocks/callers.
     */
    private static void blocksDisconnected(WorkspaceWidget w, Long socket, Long plug) {
        // Don't do anything if we're not in a procedure stack.
        Long decVar = getDecVarBlockID(plug);
        boolean socketDecVar = workspace.getEnv().getBlock(socket).isVariableDeclBlock();
        
        if (socketDecVar) {
        	VariableInfo info = myVarInfo.get(workspace.getEnv().getBlock(socket).getBlockID());
            info.initValue = false;
            //info.numTyped--;
            if (info.numTyped > 0 && info.variables.size() > 0) {
            	Block bs = workspace.getEnv().getBlock(socket);
            	BlockStub.parentVarPlugChanged(workspace, bs.getBlockID(), "poly");
                bs.setSocketAt(0, info.type, PositionType.SINGLE, "", false, false, Block.NULL);
                //workspace.getEnv().getRenderableBlock(decVar).updateConnectors();
                bs.notifyRenderable();
            }
            
        }
        if (decVar == null || !workspace.getEnv().getBlock(decVar).isVariableDeclBlock()) return;

        // Revert any output blocks in the disconnected stack. 
        VariableInfo info = myVarInfo.get(decVar);
        Block b = workspace.getEnv().getBlock(plug);
        info.variables.remove(b.getBlockID());        	
        if (info.variables.size() > 0) {
            b.getPlug().setKind(info.type);
            b.notifyRenderable();
        	return;
        }
        
        List<WorkspaceEvent> events = new ArrayList<WorkspaceEvent>();
        boolean add = false;

        
        if (info.initValue != true) {
        	info.numTyped--;
        	info.type = null;
        
            b.getPlug().setKind("poly");
            b.notifyRenderable();
        
            BlockStub.parentVarPlugChanged(workspace, decVar, "poly");
        	Block bp = workspace.getEnv().getBlock(decVar);
            bp.setSocketAt(0, "poly", PositionType.SINGLE, "", false, false, Block.NULL);
            workspace.getEnv().getRenderableBlock(decVar).updateConnectors();
            bp.notifyRenderable();
        }

        
        
        /*// If there are no more connected blocks in this procedure, remove
        // the type and revert current output blocks.
        if (info.numTyped == 0) {
            info.type = null;
            //revertType(workspace.getEnv().getBlock(decVar), info, false);
            //BlockStub.parentVarPlugChanged(workspace, decVar, "poly");
            //changeType(add, b, info.type, info, w, events);
           // changeType(info, w, events);
            //BlockStub.parentVarPlugChanged(workspace, decVar, "poly");
            Block bp = workspace.getEnv().getBlock(decVar);
           // bp.setSocketAt(0, "poly", PositionType.SINGLE, info.type, false, false, Block.NULL);
          //  workspace.getEnv().getRenderableBlock(decVar).updateConnectors();
         //   bp.notifyRenderable();

            
        }
        
        if (info.type != null) {
            // PolyRule reverts this to "poly" so we have to change it 
            // back to whatever type it should be.

            info.numTyped--;
            b.getPlug().setKind(info.type);
            b.notifyRenderable();
        }
        else
            revertType(workspace.getEnv().getBlock(plug), info, true);        */
        

    }
    
    // Output Block Types ==============================
    
    /**
     * Traverse the new portion of the stack, looking for an output type. This
     * assigns the procedure the FIRST type it encounters, regardless of which
     * is more prevalent in the stack. Only call this method if the proc does
     * not yet have an output type. 
     * 
     * @param info Filled with new output block ids 
     */
    private static void examineType(boolean add, Block b, VariableInfo info, BlockConnector sock) {
        //if (isSetter(b)) {
            if (add) {
                info.variables.add(b.getBlockID());
            }
            
            //BlockConnector socket = b.getSocketAt(0);
            //Block b2 = getBlock(socket.getBlockID());
            BlockConnector plug = b.getPlug();
            Block b2 = getBlock(plug.getBlockID());
            if (b2 != null) {
                // Found a type - set it and keep looking for blocks.
                info.numTyped++;
                if (info.type == null) 
                    info.type = plug.getKind();
                info.type = sock.getKind();
            }
            else if (!plug.getKind().equals(plug.initKind())) {
                // Reset its type, regardless of what it was before.
            	plug.setKind(plug.initKind());
                b.notifyRenderable();
            }
            
            // There are no blocks after variables.
            return;
        //}
        
/*        // Traverse sockets first.
        for (BlockConnector conn : b.getSockets()) {
            Block b2 = getBlock(conn.getBlockID());
            if (b2 != null)
                examineType(true, b2, info);
        }
        
        // Traverse after block.
        Block b2 = getBlock(b.getAfterBlockID());
        if (b2 != null)
            examineType(true, b2, info);*/
    }
    
    /**
     * Changes the output block types in the given list, disconnecting
     * blocks if necessary.
     */
    private static void changeType(VariableInfo info, 
                                   WorkspaceWidget w, List<WorkspaceEvent> e) 
    {
        String type = info.type;

        for (Long id : info.variables) {
            Block b = workspace.getEnv().getBlock(id);
            
            // If there is nothing connected to it, we just change the socket
            // type.
            BlockConnector plug = b.getPlug();
            //Block b2 = getBlock(socket.getBlockID());
            //if (b2 == null && !socket.getKind().equals(type)) {
                plug.setKind(type);
                b.notifyRenderable();
            //}
            
            // Otherwise, we might have to disconnect what's already there.
            /*else if (!socket.getKind().endsWith(type)) {
                BlockLink link = BlockLink.getBlockLink(workspace, b, b2, socket, b2.getPlug());
                link.disconnect();
                workspace.getEnv().getRenderableBlock(id).blockDisconnected(socket);
                e.add(new WorkspaceEvent(workspace, w, link, WorkspaceEvent.BLOCKS_DISCONNECTED));
            }*/
        }
    }

    /** 
     * Traverse the stack, changing any output types to the given type. 
     * Disconnect blocks if necessary.
     * 
     * @param info Filled with output block ids. 
     * @param events Filled with workspace events to be fired
     */
    private static void changeType(boolean add, 
                                   Block b, String type, VariableInfo info,
                                   WorkspaceWidget w, List<WorkspaceEvent> e) 
    {
        //if (isSetter(b)) {
            if (add) {
                info.variables.add(b.getBlockID());
            }
            
            // If there is nothing connected to it, we just change the socket
            // type.
            BlockConnector plug = b.getPlug();
            /*Block b2 = getBlock(plug.getBlockID());
            if (b2 == null && !plug.getKind().equals(type)) {*/
            	plug.setKind(type);
                b.notifyRenderable();
            //}
            
            // Otherwise, we might have to disconnect what's already there.
            /*else if (!plug.getKind().endsWith(type)) {
                // We increase the connections, even if it's the wrong type,
                // because the disconnected event will correct this.
                info.numTyped++;
                
                BlockLink link = BlockLink.getBlockLink(workspace, b, b2, plug, b2.getPlug());
                link.disconnect();
                workspace.getEnv().getRenderableBlock(b.getBlockID()).blockDisconnected(plug);
                e.add(new WorkspaceEvent(workspace, w, link, WorkspaceEvent.BLOCKS_DISCONNECTED));
            }
            
            // Otherwise, we have a connected output with the right type.
            else {
                info.numTyped++;
            }*/
        
            // There is nothing after an output block.
            return;
        //}
        
/*        // Traverse sockets first.
        for (BlockConnector conn : b.getSockets()) {
            Block b2 = getBlock(conn.getBlockID());
            if (b2 != null)
                changeType(true, b2, type, info, w, e);
        }
        
        // Traverse after block.
        Block b2 = getBlock(b.getAfterBlockID());
        if (b2 != null)
            changeType(true, b2, type, info, w, e);*/
    }
    
    /** 
     * Revert the output blocks to their original socket types, starting from
     * the given block. Updates the proc info.
     */
    private static void revertType(Block b, VariableInfo info, boolean remove) {
        //if (isSetter(b)) {
            if (remove) {
                info.variables.remove(b.getBlockID());
            }
            
            // If there is nothing connected to it, we just change the socket
            // type.
            BlockConnector socket = b.getSocketAt(0);
            Block b2 = getBlock(socket.getBlockID());
            if (b2 == null && !socket.getKind().equals(socket.initKind())) {
                socket.setKind(socket.initKind());
                b.notifyRenderable();
            }
            
            // Otherwise, decrement our connected counter.
            else if (b2 != null && remove) {
                info.numTyped--;
            }
        
            // There is nothing after an output block.
            return;
        //}
        
        /*// Traverse sockets first.
        for (BlockConnector conn : b.getSockets()) {
            Block b2 = getBlock(conn.getBlockID());
            if (b2 != null)
                revertType(b2, info, remove);
        }
        
        // Traverse after block.
        Block b2 = getBlock(b.getAfterBlockID());
        if (b2 != null)
            revertType(b2, info, remove);*/
    }
    
    /** Returns the block if one exists, or null if not. */
    public static Block getBlock(Long id) {
        if (id == null || id.equals(Block.NULL)) 
            return null;
        else {
        	if (workspace != null){
        		return workspace.getEnv().getBlock(id);
        	}
        	else {
        		return null;
        	}
        }
    }
    
    // Helper Methods =========================
    private static boolean isSetter(Block b) {
        //return SLBlockProperties.isCmd(SLCommand.CMD_OUTPUT, b);
    	return isSet("set", b);
    }
    
    
    
    // Ajout 2013
    private static Long getDecVarBlockID(Long blockID) {
    	if (blockID == null || Block.NULL.equals(blockID)  || workspace.getEnv().getBlock(blockID) == null)
			return null;
        Block b = workspace.getEnv().getBlock(blockID);
        Block bp = getParent(b);
    
        // b is the decVar block, but is it a valid type?
        if (bp != null) {
        	if (bp.isVariableDeclBlock()) return bp.getBlockID();
        }
        return null;
    }
    
    /** Returns true if "vm-cmd-name" is given cmd */
    public static boolean isSet(String cmd, Block b) {
    	//String toto = b.getProperty(VM_COMMAND_NAME);
    	//BlockGenus bg = workspace.getEnv().getGenusWithName(b.getGenusName());
    	//String titi = bg.getProperty(VM_COMMAND_NAME);
    	//return b != null && cmd.equals(bg.getProperty(VM_COMMAND_NAME));
        return b != null && cmd.equals(b.getProperty(VM_COMMAND_NAME));
    }

    public static Block getParent(Block b) {
        if (b instanceof BlockStub) {
            Block parent = ((BlockStub) b).getParent();
            if (parent != null && 
            		workspace.getEnv().getRenderableBlock(parent.getBlockID()).getParentWidget() != null) 
            {
                return parent;
            }
        }
            
        return null;
    }

    
    
    
    
    
    
    
}
