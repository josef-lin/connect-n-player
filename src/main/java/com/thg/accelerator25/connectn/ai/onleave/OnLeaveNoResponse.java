package com.thg.accelerator25.connectn.ai.onleave;

import com.thehutgroup.accelerator.connectn.player.Board;
import com.thehutgroup.accelerator.connectn.player.Counter;
import com.thehutgroup.accelerator.connectn.player.Player;


public class OnLeaveNoResponse extends Player {
  public OnLeaveNoResponse(Counter counter) {
    //TODO: fill in your name here
    super(counter, OnLeaveNoResponse.class.getName());
  }

  @Override
  public int makeMove(Board board) {
    //TODO: some crazy analysis
    //TODO: make sure said analysis uses less than 2G of heap and returns within 10 seconds on whichever machine is running it
    long startTime = System.currentTimeMillis();
    long elapsedTime = 0L;
    System.out.println("attempting to call Yibo");
    while (elapsedTime < 9999) {
      elapsedTime = System.currentTimeMillis() - startTime;
    }
    System.out.println("elapsed time:: " + elapsedTime);
    System.out.println("no response");
    startTime = System.currentTimeMillis();
    elapsedTime = 0L;
    while (elapsedTime < 1000) {
      elapsedTime = System.currentTimeMillis() - startTime;
    }
    return 4;
  }
}