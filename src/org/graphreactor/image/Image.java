package org.graphreactor.image;

// TODO to push the experiments below up to the limits of the system/mem/proc/neo
// and then re-design the whole process w CUDA on C++ or Java? - 
// TODO check if CUDA is available for Java/OSX

import static org.neo4j.kernel.impl.util.FileUtils.deleteRecursively;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

public class Image {
	
	Integer dimX = 11;
	Integer dimY = 11;
	
	private static final String DB_PATH = "../DBs/Image";
	// START SNIPPET: createReltype
	public static enum RelTypes implements RelationshipType
	{
		X,
		Y,
		Z
	}
	// END SNIPPET: createReltype
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Image image = new Image();
		image.run();
	}

	void run() {
		
		clearDbPath(DB_PATH);
		
		// START SNIPPET: addData
		GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase( DB_PATH );
		
		Label pixel = DynamicLabel.label("Pixel");
		
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
		
		try ( Transaction tx = db.beginTx())
		{   
			Node[][] p = new Node[dimX][dimY];
			
			for (int i = 0; i < dimX; i++) {
				for (int j = 0; j < dimY; j++) {
					p[i][j] = db.createNode(pixel);
					p[i][j].setProperty( "x", i );
					p[i][j].setProperty( "y", j );
					p[i][j].setProperty( "R", 0 );
					p[i][j].setProperty( "G", 0 );
					p[i][j].setProperty( "B", 0 );
				}				
			}
			
			System.out.println("Nodes created");
			
			// TODO to write only in the nodes (and all rels linked to that node - 
			// maybe on a radius r) where RGB values are chaanged from previous frame
			// TODO to experiment the image change threshold in such way that only a 
			// fraction - %2 ? of pixels will be changed in order to give enough time
			// to write all pixels changed in one frame
			
			Relationship xRel;
			Relationship yRel;
			Long nrRel = 0L;
			Long tStartRel = Instant.now().toEpochMilli();
			
			for (int i = 0; i < dimX-1; i++) {
				for (int j = 0; j < dimY; j++) {
					xRel = p[i][j].createRelationshipTo( p[i+1][j], RelTypes.X);
					long t = Instant.now().toEpochMilli();
					nrRel++;
					xRel.setProperty("t",t);
					System.out.println(nrRel + " xRel time t = " + t);
				}				
			}
			System.out.println("Relations X created");
			for (int i = 0; i < dimX; i++) {
				for (int j = 0; j < dimY-1; j++) {
					yRel = p[i][j].createRelationshipTo(p[i][j+1], RelTypes.Y);
					long t = Instant.now().toEpochMilli();
					nrRel++;
					yRel.setProperty("t",t);
					System.out.println(nrRel + " yRel time t = " + t);
				}				
			}
			Long tEndRel = Instant.now().toEpochMilli();
			Long tRelDuration = tEndRel - tStartRel;
			System.out.println("Relations Y created");
			System.out.println(nrRel + " Relations created in " + tRelDuration + "ms with avg: " + tRelDuration/nrRel + "Rel/ms");

			tx.success();
		}        
		// END SNIPPET: addData
		
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

}
