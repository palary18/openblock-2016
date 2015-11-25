package edu.mit.blocks.codeblocks;

import edu.mit.blocks.renderable.Comment;
import edu.mit.blocks.renderable.RenderableBlock;
import edu.mit.blocks.workspace.Workspace;
import edu.mit.blocks.workspace.WorkspaceEvent;


public class CommentBlock {

    protected final Workspace workspace;

    public CommentBlock(Workspace workspace) {
        this.workspace = workspace;
    }
    public void ComBlock(Block block)//, WorkspaceEvent event)
	{
		if (block != null && (
				block.getGenusName().equals("setterlcdinit") || 
				block.getGenusName().equals("lcdinit"))) {
        	RenderableBlock rb = workspace.getEnv().getRenderableBlock(block.getBlockID());
        	
        	rb.addComment();
        	Comment c = rb.getComment();
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
		}
		
		if (block != null && (
				block.getGenusName().equals("qcmd-sendtoback") || 
				block.getGenusName().equals("qcmd-sendtofront") ||
				block.getGenusName().equals("qcmd-receive") ||
				block.getGenusName().equals("qcmd-overwrite") ||
				block.getGenusName().equals("qcmd-peek") ||
				block.getGenusName().equals("qcmd-reset") ||
				block.getGenusName().equals("qOK-sendtoback") || 
				block.getGenusName().equals("qOK-sendtofront") ||
				block.getGenusName().equals("qOK-receive") ||
				block.getGenusName().equals("qOK-overwrite") ||
				block.getGenusName().equals("qOK-peek") ||
				block.getGenusName().equals("qOK-reset"))) {
        	RenderableBlock rb = workspace.getEnv().getRenderableBlock(block.getBlockID());
        	
        	rb.addComment();
        	Comment c = rb.getComment();
        	c.setText("- enqueue FI : post an item on the queue ;\n" +
        			"- push LI : post an item on the stack ;\n" +
        			"- dequeue/pop FO : remove a data from the queue/stack and copy it into a buffer ;\n" +
        			"- copy FO : copy into a buffer a data from the queue/stack without removing it ;\n" +
        			"- overwrite : overwrite data in queue intended for use with queues that have a length of one ;\n" +
        			"- reset : resets a queue/stack to its original empty state. ");//getCommentSource();
        	c.setSize(600, 400);
        	//c.setBounds(c.getX(), c.getY(), 20, 10);//.setSize(200, 100);
        	c.reformComment();
        	c.setVisible(true);
        	c.update(false);
        	c.setZoomLevel(1.0);
		}
		
		if (block != null && (block.getGenusName().equals("qhandle"))) {
        	RenderableBlock rb = workspace.getEnv().getRenderableBlock(block.getBlockID());
        	
        	rb.addComment();
        	Comment c = rb.getComment();
        	c.setText("Stack is a LIFO (last in first out) data structure.\n" +
        			"Queue is a FIFO (first in first out) data structure.\n" +
        			"and 'lenght' is the maximum number of items that the queue can contain.\n");//getCommentSource();
        	c.setSize(600, 400);
        	//c.setBounds(c.getX(), c.getY(), 20, 10);//.setSize(200, 100);
        	c.reformComment();
        	c.setVisible(true);
        	c.update(false);
        	c.setZoomLevel(1.0);
		}		
		
		if (block != null && (
				block.getGenusName().equals("semhandle") || 
				block.getGenusName().equals("queuestack"))) {
        	RenderableBlock rb = workspace.getEnv().getRenderableBlock(block.getBlockID());
        	
        	rb.addComment();
        	Comment c = rb.getComment();
        	c.setText("set the time to wait :\n" +
        			"  0 : no wait ;\n" +
        			"> 0 : wait in ms ;\n" +
        			"no number : block indefinitely.\n" +
        			"\n" + 
        			"superflous for a 'give semaphore'");//getCommentSource();
        	c.setSize(600, 400);
        	//c.setBounds(c.getX(), c.getY(), 20, 10);//.setSize(200, 100);
        	c.reformComment();
        	c.setVisible(true);
        	c.update(false);
        	c.setZoomLevel(1.0);
		}
		if (block != null && (block.getGenusName().equals("semdeclare"))) {
        	RenderableBlock rb = workspace.getEnv().getRenderableBlock(block.getBlockID());
        	
        	rb.addComment();
        	Comment c = rb.getComment();
        	c.setText("Semaphore is used for controlling access to a common resource in a parallel programming.\n" +
        			"This allows problem solving synchronization tasks.\n" +
        			"set the kind of semaphore  by the \"type\" number :\n" +
        			"  1 : binary semaphore ;\n" +
        			"> 1 : counting semaphore ;\n" +
        			"no number : mutex semaphore.\n\n");//getCommentSource();
        	c.setSize(600, 400);
        	//c.setBounds(c.getX(), c.getY(), 20, 10);//.setSize(200, 100);
        	c.reformComment();
        	c.setVisible(true);
        	c.update(false);
        	c.setZoomLevel(1.0);
		}
	}
}
