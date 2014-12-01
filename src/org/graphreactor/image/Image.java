package org.graphreactor.image;

// TODO to push the experiments below up to the limits of the system/mem/proc/neo
// and then re-design the whole process w CUDA on C++ or Java? - 
// TODO check if CUDA is available for Java/OSX
// TODO to write RGB concatenated as strings? as property value for
// a time property named as a toString time Long on milliseconds
// this way we can keep a history of the image/pixel colors in time
// TODO to experiment with variations of time eventually with 2 time values:
// one for story/un-interrupted sequence of change and or
// one for frame and or one for every single node and rel
// TODO only Nodes and linked rels for the pix/voxels with change RGB
// different of 0 will be written - not full frame
// TODO to write video feed writer
// TODO to write memory story segmenter to identify smaller recurring
// patterns which then will get worded codes
// TODO to write a "forecaster" query interleaved with memory writer
// which will check matches with existing memories and return "expected"
// next frames

import static org.neo4j.kernel.impl.util.FileUtils.deleteRecursively;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;


class FacePanel extends JPanel{  
    private static final long serialVersionUID = 1L;  
    private BufferedImage image;  
    // Create a constructor method  
    public FacePanel(){  
         super();   
    }  
    /*  
     * Converts/writes a Mat into a BufferedImage.  
     *   
     * @param matrix Mat of type CV_8UC3 or CV_8UC1  
     * @return BufferedImage of type TYPE_3BYTE_BGR or TYPE_BYTE_GRAY  
     */       
    public boolean matToBufferedImage(Mat matrix) {  
         MatOfByte mb=new MatOfByte();  
         Highgui.imencode(".jpg", matrix, mb);  
         try {  
              this.image = ImageIO.read(new ByteArrayInputStream(mb.toArray()));  
         } catch (IOException e) {  
              e.printStackTrace();  
              return false; // Error  
         }  
      return true; // Successful  
    }  
    public void paintComponent(Graphics g){  
         super.paintComponent(g);   
         if (this.image==null) return;         
          g.drawImage(this.image,0,0,this.image.getWidth(),this.image.getHeight(), null);
    }
       
}  

public class Image {

	Integer dimX = 100;
	Integer dimY = 100;
	Node[][] p = new Node[dimX][dimY];
	Relationship[][] xRel = new Relationship[dimX-1][dimY];
	Relationship[][] yRel = new Relationship[dimX][dimY-1];
	Relationship tRel;
	
	// Threshold on the gray level to filter the changed pixels
	Integer threshold = 30;									

	private static final String DB_PATH = "../DBs/Image";	
	GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase( DB_PATH );

	Label pixelLabel = DynamicLabel.label("Pixel");
	Label frameLabel = DynamicLabel.label("Frame");
	Label sequenceLabel = DynamicLabel.label("Sequence");	

	// Create Relationships types
	public static enum RelTypes implements RelationshipType
	{
		X,
		Y,
		T
	}
	
	public static void main(String[] args) throws InterruptedException {
		// TODO Auto-generated method stub
		Image image = new Image();
		image.run();
	}

	void run() throws InterruptedException {

//		System.out.println("Cleanup Graph DB at: " + DB_PATH);
//		clearDbPath(DB_PATH);
//		System.out.println("Graph DB clean.");
		
		initializeGraphNet();

		// Load the native library.
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		//make the JFrame
		JFrame frame = new JFrame("WebCam Capture");  
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  

		FacePanel facePanel = new FacePanel();  
		frame.setSize(dimX,dimY); //set size 
		frame.setLocationRelativeTo(null); //set position to screen center
		frame.setBackground(Color.BLUE);
		frame.add(facePanel,BorderLayout.CENTER);       
		frame.setVisible(true);       

		//Open and Read from the video stream  
		Mat img=new Mat();  
		Mat img_gray=new Mat();  
		Mat img_prev=new Mat();  
		Mat img_prev_gray=new Mat();  
		Mat img_diff=new Mat();  
		Mat img_diff_gray=new Mat(); 
		Mat img_diff_channels=new Mat(); 
		Mat img_diff_color=new Mat(); 
		List<Mat> imgList = new ArrayList<Mat>();

		Integer changedPixels = 0;					// to host nr of pixels with color values changed
		Integer frameId = 0;
		Integer sequenceId = 0;

		Long timeMilli = 0L;

		VideoCapture webCam =new VideoCapture(2);   // set the webCam to use if more available

		if( webCam.isOpened())  
		{  
			Thread.sleep(100); /// This one-time delay allows the Webcam to initialize itself  

			webCam.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, dimX);
			webCam.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, dimY);

			webCam.read(img_prev);
			webCam.read(img);

			while( true )  
			{  
				webCam.read(img);  
				if( !img.empty() )  
				{   
					Imgproc.cvtColor(img, img_gray, Imgproc.COLOR_RGB2GRAY);
					Imgproc.cvtColor(img_prev, img_prev_gray, Imgproc.COLOR_RGB2GRAY);	            	 
					Core.absdiff(img_gray, img_prev_gray, img_diff_gray);
					Imgproc.threshold(img_diff_gray, img_diff, threshold, 1, Imgproc.THRESH_BINARY);

					changedPixels = Core.countNonZero(img_diff);
					System.out.println("Changed pixels: " + changedPixels);

					imgList.add(0, img_diff);
					imgList.add(1, img_diff);
					imgList.add(2, img_diff);

					Core.merge(imgList, img_diff_channels);
					imgList.clear();

					Core.multiply(img_diff_channels, img, img_diff_color);

					webCam.read(img_prev);
					//Display the image  
					facePanel.matToBufferedImage(img_diff_color);  
					facePanel.repaint();  

					//	            	   double[] px = img_diff_color.get(0, 0); 
					//	                   System.out.println("B: "+px[0] + " G: " + px[1]+ " R: " + px[2] + " Frame|pixel time: " + tEndRel);	

					timeMilli = Instant.now().toEpochMilli();

					// IF changedEntities-Pixels (something changed) > 0 AND frameId == 0 (first frame in a new session)
					// THEN Start first Sequence (Create Sequence node) AND Create Frame node AND Write full frame in DB	                  
					if ( (changedPixels > 0) && (frameId == 0) ) {
						sequenceId++;
						frameId++;	   

						//START SNIPPET: add first Frame in Sequence to graph database
						try ( Transaction tx = db.beginTx())
						{   
							System.out.println(" - Write to DB first Frame in first Sequence - start time: " + timeMilli );

							Node sequenceNode = db.createNode(sequenceLabel);
							sequenceNode.setProperty("sequenceId", sequenceId);
							sequenceNode.setProperty("nrFrames", frameId);
							sequenceNode.setProperty("changedEntities", changedPixels);
							sequenceNode.setProperty("tStart", timeMilli);

							Node frameNode = db.createNode(frameLabel);
							frameNode.setProperty("frameId", frameId);
							frameNode.setProperty("changedPixels", changedPixels);
							frameNode.setProperty("t", timeMilli);

							for (int i = 0; i < dimX; i++) {
								for (int j = 0; j < dimY; j++) {
									p[i][j].setProperty( "B", img.get(i, j)[0] );
									p[i][j].setProperty( "G", img.get(i, j)[1] );
									p[i][j].setProperty( "R", img.get(i, j)[2] );
									// maybe to calculate and write also gray level
								}
							}
							// System.out.println("Nodes created");	                		   
							// ? To add IN relations for pixelNodes IN frame Node IN sequenceNode

							for (int i = 0; i < dimX-1; i++) {
								for (int j = 0; j < dimY; j++) {
									xRel[i][j].setProperty("f",frameId);
								}
							}

							for (int i = 0; i < dimX; i++) {
								for (int j = 0; j < dimY-1; j++) {
									yRel[i][j].setProperty("f",frameId);
								}
							}
							tx.success();
							Long tEnd = Instant.now().toEpochMilli();
							System.out.println(" - Wrote to DB first Frame in first Sequence - in ms: " +  (tEnd - timeMilli));

						}
						//END SNIPPET: add first Frame in Sequence to graph database	                	   

					}

				}  
				else  
				{   
					System.out.println(" --(!) No captured frame from webcam !");   
					break;   
				}  
			}  
		}
		webCam.release(); //release the webcam		
	}
	
	private void clearDbPath(String path)
	{
		try
		{
			deleteRecursively( new File( path ) );
		}
		catch ( IOException e )
		{
			throw new RuntimeException( e );
		}
	}

	private void initializeGraphNet() {

		//Create a Graph Network with Nodes and Relations 				
		System.out.println("Creating a Graph Network...");
		Long tStart = Instant.now().toEpochMilli();
		
		try ( Transaction tx = db.beginTx())
		{   		    		   
			for (int i = 0; i < dimX; i++) {
				for (int j = 0; j < dimY; j++) {
					p[i][j] = db.createNode(pixelLabel);
					p[i][j].setProperty( "x", i );
					p[i][j].setProperty( "y", j );
				}
			}
			System.out.println("Nodes created: " + dimX*dimY);	                		   

			for (int i = 0; i < dimX-1; i++) {
				for (int j = 0; j < dimY; j++) {
					xRel[i][j] = p[i][j].createRelationshipTo( p[i+1][j], RelTypes.X);
				}
			}
			System.out.println("xRel created: " + (dimX-1)*dimY);
			
			for (int i = 0; i < dimX; i++) {
				for (int j = 0; j < dimY-1; j++) {
					yRel[i][j] = p[i][j].createRelationshipTo(p[i][j+1], RelTypes.Y);
				}
			}
			System.out.println("yRel created: " + dimX*(dimY-1));
			
			tx.success();
			Long tEnd = Instant.now().toEpochMilli();
			System.out.println("Graph network created with " + dimX*dimY + " Nodes and " + ((dimX-1)*dimY + dimX*(dimY-1)) + " Relations in ms: " + (tEnd-tStart) );
		}
	}
}