package org.graphreactor.image;

import static org.neo4j.kernel.impl.util.FileUtils.deleteRecursively;

import java.io.File;
import java.io.IOException;
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

public class Cube {
	
	Integer dimX = 4;
	Integer dimY = 3;
	Integer dimZ = 2;
	
	private static final String DB_PATH = "../DBs/Cube";
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
		Cube image = new Cube();
		image.run();
	}

	void run() {
		
		clearDbPath(DB_PATH);
		
		// START SNIPPET: addData
		GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase( DB_PATH );
		
		Label pixel = DynamicLabel.label("Voxel");
		
		try ( Transaction tx = db.beginTx())
		{   
			Node[][][] p = new Node[dimX][dimY][dimZ];
			
			for (int i = 0; i < dimX; i++) {
				for (int j = 0; j < dimY; j++) {
					for (int k = 0; k < dimZ; k++) {
					p[i][j][k] = db.createNode(pixel);
					p[i][j][k].setProperty( "x", i );
					p[i][j][k].setProperty( "y", j );
					p[i][j][k].setProperty( "z", j );
					p[i][j][k].setProperty( "R", 0 );
					p[i][j][k].setProperty( "G", 0 );
					p[i][j][k].setProperty( "B", 0 );
					}
				}				
			}
			
			System.out.println("Nodes created");
			
			Relationship xRel;
			Relationship yRel;
			Relationship zRel;
			
			for (int i = 0; i < dimX-1; i++) {
				for (int j = 0; j < dimY; j++) {
					for (int k = 0; k < dimZ; k++) {
						xRel = p[i][j][k].createRelationshipTo( p[i+1][j][k], RelTypes.X);
					}	
				}				
			}
			System.out.println("Relations X created");
			for (int i = 0; i < dimX; i++) {
				for (int j = 0; j < dimY-1; j++) {
					for (int k = 0; k < dimZ; k++) {
						xRel = p[i][j][k].createRelationshipTo(p[i][j+1][k], RelTypes.Y);
					}	
				}				
			}
			System.out.println("Relations Y created");
			for (int i = 0; i < dimX; i++) {
				for (int j = 0; j < dimY; j++) {
					for (int k = 0; k < dimZ-1; k++) {
						xRel = p[i][j][k].createRelationshipTo(p[i][j][k+1], RelTypes.Z);
					}
				}				
			}
			System.out.println("Relations Z created");

			tx.success();
		}        
		// END SNIPPET: addData
	db.shutdown();	
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
