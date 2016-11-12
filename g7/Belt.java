package sqdance.g7;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import sqdance.sim.Point;

public class Belt {
	int numDancers;
	Map<Integer,Point> indexToPositionSide;
	ArrayList<Dancer> dancerList;
	
	Point[][] tablePositions;
	
	boolean beltParity = false;
	
	private static int MaxNumRows = 19;
	private static int MaxNumCols = 39;
	private static int MaxPairNum = 2;
	
	public Belt(int numDancers){
		if(numDancers%2!=0)
			throw new RuntimeException("Number of Dancers must be even.");
		
		this.numDancers = numDancers;
		
		initializeTablePositions(numDancers+1);
		
		indexToPositionSide = new HashMap<Integer,Point>();
		dancerList = new ArrayList<Dancer>();
		
		int numCols = MaxNumCols;
		int numRows = (numDancers/2)/numCols * 2;
		numRows += (numDancers%numCols==0)? 0 : 2;
		
		
		for(int i=0; i<numDancers; i++){
			
			boolean onTopRow = (i < numDancers/2);
			boolean goingRight = (i/numCols)%2 == 0;
			
			if(onTopRow && goingRight)
				indexToPositionSide.put(i, tablePositions[(i/numCols)*2][i%numCols]);
			else if(onTopRow && !goingRight)
				indexToPositionSide.put(i, tablePositions[(i/numCols)*2][numCols -1 - i%numCols]);
			else if(!onTopRow && goingRight)
				indexToPositionSide.put(i, tablePositions[(numRows-(i/numCols))*2 -1][i%numCols]);
//			else if(!onTopRow && !goingRight)
//				dfgdfgdfg
			else
				indexToPositionSide.put(i, tablePositions[5][0]);
		}
		for(int i=0; i<numDancers;i++){
			dancerList.add(new Dancer(i, i));
		}
		
	}
	
	public void initializeTablePositions(int numDancers){
		tablePositions = new Point[38][39];
		
		for(int row=0; row<38; row++){
			for(int column=0; column<39; column++){
				double offSet = (0.001) * row/2;
				tablePositions[row][column] = new Point(column*0.51,row*0.5 + offSet);
			}
		}
	}
	
	public Point getPosition(int i){
		return indexToPositionSide.get(i);
	}
	
	private boolean cycleParity=true;
	public Point[] spinBelt(){
		Point[] instructions = new Point[numDancers];
		
		if(cycleParity){
			for(int i=0; i<numDancers;i++){
				Dancer oldDancer = dancerList.get(i);
				if(oldDancer.beltIndex<numDancers/2+1){
					//Dancer oldDancer = dancerList.get(i);
					int oldBeltIndex = oldDancer.beltIndex; 
					int newBeltIndex = oldDancer.beltIndex+1;
					Point oldPos = getPosition(oldBeltIndex);
					Point newPos = getPosition(newBeltIndex);
					oldDancer.beltIndex = newBeltIndex;
					Point instruct = new Point(newPos.x-oldPos.x, newPos.y-oldPos.y); 
					instructions[i] = instruct;
				}
			}
			cycleParity = !cycleParity;
		}
		else{
			for(int i=0; i<numDancers;i++){
				Dancer oldDancer = dancerList.get(i);
				int oldBeltIndex = oldDancer.beltIndex; 
				
				int newBeltIndex;
				if(oldBeltIndex==numDancers)
					newBeltIndex=1;
				else{
					Dancer newDancer = dancerList.get(i+1);
					newBeltIndex = newDancer.beltIndex;
				}
				
				Point oldPos = getPosition(oldBeltIndex);
				Point newPos = getPosition(newBeltIndex);
				oldDancer.beltIndex = newBeltIndex;
				Point instruct = new Point(newPos.x-oldPos.x, newPos.y-oldPos.y); 
				instructions[i] = instruct;
			}
			cycleParity = !cycleParity;
		}
		return instructions;
	}
	
}
 
