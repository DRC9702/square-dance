package sqdance.g7;

import sqdance.sim.Point;

import java.io.*;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static java.util.Comparator.comparing;

public class Player implements sqdance.sim.Player {

	// E[i][j]: the remaining enjoyment player j can give player i
	// -1 if the value is unknown (everything unknown upon initialization)
	private int[][] E = null;

	// random generator
	private Random random = null;

	// simulation parameters
	private int d = -1;
	private double room_side = -1; 


	private int[] idle_turns;

	private Belt belt;

	
	private static  int NUM_DANCE_TURNS = 4; // Only use (1,2,5,10)-1
	
	private static int lowestDancers = -1;

	private int danceTurn;

	private SoulmateMatching soulmateMatch = new SoulmateMatching();

	private static final int soulmateThreshold = 812;

	// init function called once with simulation parameters before anything else is called
	public void init(int d, int room_side) {
		this.d = d;
		this.room_side = (double) room_side;
		random = new Random();
		if (d <= soulmateThreshold) {
			soulmateMatch.init(d, room_side);
		}

		E = new int [d][d];
		idle_turns = new int[d];
		for (int i=0 ; i<d ; i++) {
			idle_turns[i] = 0;
			for (int j=0; j<d; j++) {
				E[i][j] = i == j ? 0 : -1;
			}
		}

		if (d <= 1716) {
			NUM_DANCE_TURNS = 19;	
		} else if (d > 26400){
			NUM_DANCE_TURNS = 1;	
		} else {
		int numDancers = d;
		int numExtraDancers = (numDancers > 1716) ? numDancers - 1716: 0;
		int numNeededBlocks = (numExtraDancers%24==0) ? numExtraDancers/24 : numExtraDancers/24 + 1;
		int halfBlocksPerRow = (numNeededBlocks%88==0) ? numNeededBlocks/88 : numNeededBlocks/88 + 1;
		boolean overflow = (numNeededBlocks%88!=0);
		int dancersInLastCol = (!overflow) ? 24 : (halfBlocksPerRow==1)? numExtraDancers : numExtraDancers%(24*88*(halfBlocksPerRow-1));
		int dancersInLastColBlock = (dancersInLastCol%88==0) ? dancersInLastCol/88 : dancersInLastCol/88 + 1;
		int posToMove = 2*( (halfBlocksPerRow-1)*24 + dancersInLastColBlock) + (39 - 2*(halfBlocksPerRow));
		NUM_DANCE_TURNS = (1800-posToMove)/posToMove;
			//double k = (19.0-1.0)/(26400-1716);
			//NUM_DANCE_TURNS = (int)(19 - k * (d - 1716));
		}
		
		danceTurn = NUM_DANCE_TURNS > 19 ? 19 : NUM_DANCE_TURNS;
		System.out.println(danceTurn);
	}

	// setup function called once to generate initial player locations
	// note the dance caller does not know any player-player relationships, so order doesn't really matter in the Point[] you return. Just make sure your player is consistent with the indexing

	public Point[] generate_starting_locations() {
		if (d <= soulmateThreshold) {
			return soulmateMatch.generate_starting_locations();
		}

		belt = new Belt(d);	
		Point[] L = new Point[d];
		for(int i=0; i<d; i++){
			L[i] = belt.getPosition(belt.dancerList.get(i).beltIndex);
		}
		lowestDancers = belt.recommendedLowestDancerNumber;
		
		return L;
	}
    

	// play function
	// dancers: array of locations of the dancers
	// scores: cumulative score of the dancers
	// partner_ids: index of the current dance partner. -1 if no dance partner
	// enjoyment_gained: integer amount (-5,0,3,4, or 6) of enjoyment gained in the most recent 6-second interval
	
	public static int[] bottomIndices(final int[] input, final int n){
		return IntStream.range(0,input.length).boxed()
				.sorted(comparing(i->input[i])).mapToInt(i -> i)
				.limit(n).toArray();			
	}
	
	public Point[] play(Point[] dancers, int[] scores, int[] partner_ids, int[] enjoyment_gained) {
		if (d <= soulmateThreshold) {
			return soulmateMatch.play(dancers, scores, partner_ids, enjoyment_gained);
		}
		
		//System.out.println(Arrays.toString(bottomIndices(scores,scores.length/10)));
		
		
		Point[] instructions = new Point[d];	
		for(int i=0; i<d; i++)
			instructions[i] = new Point(0,0);

		if(danceTurn == 0){
			int[] lowestScorers = bottomIndices(scores,lowestDancers);
			List<Integer> lowScorerList = IntStream.of(lowestScorers).boxed().collect(Collectors.toList());
			for(Dancer d : belt.dancerList){
				if(lowScorerList.contains(d.dancerId))
					d.isLowerScorer=true;
				else
					d.isLowerScorer=false;
			}
			
			
			//System.out.println("MOVE");
			setEveryoneToDance();	// Set everyone's status to be "WILL_Dance" (was dancing)
			Set<Integer> curDancers = getCurDancers(enjoyment_gained);
			instructions = belt.spinBelt(curDancers);
			danceTurn = NUM_DANCE_TURNS + 1;
		} else {
			//System.out.println("Dance");
			addDanceTimeToEveryone(enjoyment_gained);
			
		}
		

		danceTurn--;


		for (int i=0 ; i<d ; ++i)
			if(dancers[i].x + instructions[i].x < 0)
				System.out.printf("Dancer[%d], BeltIndex[%d], Pos[%f,%f], Instruct[%f,%f]\n", i, belt.dancerList.get(i).beltIndex, dancers[i].x, dancers[i].y,instructions[i].x,instructions[i].y);
		return instructions;
	}

	private void addDanceTimeToEveryone(int[] enjoyment_gained) {
		for (Dancer d : belt.dancerList) {
			int partnerId = belt.getPartnerDancerID(d.dancerId);
			d.classifyDancer(partnerId,enjoyment_gained[d.dancerId]);
		}
	}
	
	private void setEveryoneToDance() {
		for (Dancer d : belt.dancerList) {
			belt.changeDancerStatus(d.dancerId, Dancer.WILL_DANCE);
		}
	}
	
	private Set<Integer> getCurDancers(int[] enjoyment_gained) {
		Set<Dancer> res_dancers = new HashSet<>();
		for (Dancer d : belt.dancerList) {
			int partnerId = belt.getPartnerDancerID(d.dancerId);
			
			if (d.dancerStatus == Dancer.WILL_DANCE) {
				d.classifyDancer(partnerId, enjoyment_gained[d.dancerId]);
			} 

			d.determineStatus(partnerId);

			if (d.dancerStatus == Dancer.WILL_DANCE) {
				res_dancers.add(d);
			}
		}

		Set<Integer> res = belt.verifyDancer(res_dancers);
		return res;
	}

	private int total_enjoyment(int enjoyment_gained) {
		switch (enjoyment_gained) {
		case 3: return 60; // stranger
		case 4: return 200; // friend
		case 6: return 10800; // soulmate
		default: throw new IllegalArgumentException("Not dancing with anyone...");
		}	
	}
}
