package edu.mit.blocks.codeblocks;

import edu.mit.blocks.renderable.Comment;
import edu.mit.blocks.renderable.RenderableBlock;
import edu.mit.blocks.workspace.Workspace;
import edu.mit.blocks.workspace.WorkspaceEvent;
import edu.mit.blocks.workspace.WorkspaceListener;
import edu.mit.blocks.codeblocks.CommentBlock;

public class CommandRule implements LinkRule, WorkspaceListener {
    
    private final Workspace workspace;

    public CommandRule(Workspace workspace) {
        this.workspace = workspace;
    }

    public boolean canLink(Block block1, Block block2, BlockConnector socket1, BlockConnector socket2) {
        if (!BlockConnectorShape.isCommandConnector(socket1) || !BlockConnectorShape.isCommandConnector(socket2)) {
            return false;
        }
        // We want exactly one before connector
        if (socket1 == block1.getBeforeConnector()) {
            return !socket1.hasBlock();
        } else if (socket2 == block2.getBeforeConnector()) {
            return !socket2.hasBlock();
        }
        return false;
    }

    public boolean isMandatory() {
        return false;
    }

    public void workspaceEventOccurred(WorkspaceEvent e) {
        // TODO Auto-generated method stub
        if (e.getEventType() == WorkspaceEvent.BLOCKS_CONNECTED) {
            BlockLink link = e.getSourceLink();
            if (link.getLastBlockID() != null && link.getLastBlockID() != Block.NULL
                    && BlockConnectorShape.isCommandConnector(link.getPlug()) && BlockConnectorShape.isCommandConnector(link.getSocket())) {
                Block top = workspace.getEnv().getBlock(link.getPlugBlockID());
                while (top.hasAfterConnector() && top.getAfterConnector().hasBlock()) {
                    top = workspace.getEnv().getBlock(top.getAfterBlockID());
                }
                Block bottom = workspace.getEnv().getBlock(link.getLastBlockID());

                // For safety: if either the top stack is terminated, or
                // the bottom stack is not a starter, don't try to force a link
                if (!top.hasAfterConnector() || !bottom.hasBeforeConnector()) {
                    return;
                }

                link = BlockLink.getBlockLink(workspace, top, bottom, top.getAfterConnector(), bottom.getBeforeConnector());
                link.connect();
            }
        }
        if (e.getEventType() == WorkspaceEvent.BLOCK_ADDED) {
        	CommentBlock cBlock = new CommentBlock(workspace);
        	
        	Block b = workspace.getEnv().getBlock(e.getSourceBlockID());
            BlockLink link = e.getSourceLink();
            cBlock.ComBlock(b);
            /*if (b != null && (b.getGenusName().equals("setterlcdinit") || b.getGenusName().equals("lcdinit"))) {
            	RenderableBlock rb = workspace.getEnv().getRenderableBlock(e.getSourceBlockID());
            	long idd = e.getSourceBlockID();
            	//String rs = b.getProperty("RS");
            	//String rw = b.getProperty("RW");
            	rb.addComment();
            	Comment c = rb.getComment();//.setComment("toto");
            	c.setText("'#' character gives access to LCD functions.\n" +
            			"#105 : Positions the cursor at row 1 and column 5\n" +
            			"#213 : Positions the cursor at row 2 and column 13\n" +
            			"#h : Positions the cursor at home, in the upper-left of the LCD. #h = #101.\n" +
            			"#c : Clears the screen. Puts the cursor at home.\n" +
            			"#U : Displays an underscore cursor.\n" +
            			"#u : Hides the LCD cursor.\n" +
            			"#B : Displays a blinking cursor.\n" +
            			"#b : Turns off the blinking cursor.\n" +
            			"#D : Turns on the LCD display.\n" +
            			"#d : Turns off the LCD display.\n" +
            			"#A : Turns on automatic scrolling.\n" +
            			"#a : Turns off automatic scrolling.\n" +
            			"#L : Scrolls one space to the Left.\n" +
            			"#R : Scrolls one space to the Right.\n" +
            			"#l : Sets the direction for text written to left-to-right, the default.\n" +
            			"#r : Sets the direction for text written to right-to-left.\n");//getCommentSource();
            	c.setSize(200, 100);
            	//c.setBounds(c.getX(), c.getY(), 20, 10);//.setSize(200, 100);
            	c.reformComment();
            	c.setVisible(true);
            	c.update(false);
            	c.setZoomLevel(1.0);
            	
            }*/
        }
    }
}
