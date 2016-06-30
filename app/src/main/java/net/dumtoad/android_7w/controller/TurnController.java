package net.dumtoad.android_7w.controller;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.dumtoad.android_7w.R;
import net.dumtoad.android_7w.cards.Card;
import net.dumtoad.android_7w.cards.CardCollection;
import net.dumtoad.android_7w.cards.Hand;
import net.dumtoad.android_7w.cards.Player;
import net.dumtoad.android_7w.dialog.PassThePhone;
import net.dumtoad.android_7w.fragment.WonderFragment;
import net.dumtoad.android_7w.view.CardView;

public class TurnController {
    private MasterViewController mvc;
    private TradeController tradeController;
    private int playerTurn;
    private int playerViewing;
    private Mode mode;

    private enum Mode {
        wonder,
        summary,
        handtrade
    }

    public TurnController(MasterViewController mvc) {
        this.mvc = mvc;
        tradeController = new TradeController(mvc);
        this.mode = Mode.wonder;
    }

    public TurnController(MasterViewController mvc, Bundle savedInstanceState) {
        this.mvc = mvc;
        tradeController = new TradeController(mvc, savedInstanceState.getBundle("tradeController"));
        this.playerTurn = savedInstanceState.getInt("playerTurn");
        this.playerViewing = savedInstanceState.getInt("playerViewing");
        this.mode = Mode.valueOf(savedInstanceState.getString("mode"));
    }

    public Bundle getInstanceState() {
        Bundle outstate = new Bundle();
        outstate.putInt("playerTurn", playerTurn);
        outstate.putInt("playerViewing", playerViewing);
        outstate.putString("mode", mode.toString());
        outstate.putBundle("tradeController", tradeController.getInstanceState());
        return outstate;
    }

    public TradeController getTradeController() {
        return tradeController;
    }

    public Player getCurrentPlayer() {
        return mvc.getPlayer(playerTurn);
    }

    public void startTurn(int playerNum) {
        tradeController = new TradeController(mvc);
        this.playerTurn = this.playerViewing = playerNum;
        if(mvc.getPlayer(playerNum).isAI()) {
            //Do something cool!
        } else {
            if(mvc.getTableController().getNumHumanPlayers() > 1) {
                DialogFragment df = new PassThePhone();
                Bundle args = new Bundle();
                args.putString("name", mvc.getPlayer(playerNum).getName());
                df.setArguments(args);
                df.show(mvc.getActivity().getFragmentManager(), "passthephone");
            }
            WonderFragment wf = new WonderFragment();
            mvc.getActivity().getFragmentManager().beginTransaction()
                    .replace(R.id.main_layout, wf, "WonderSelect")
                    .commit();
        }
    }

    //west is true, east is false
    public void go(boolean direction) {
        playerViewing = getPlayerDirection(playerViewing, direction);
        showMode();
    }

    private int getPlayerDirection(int start, boolean direction) {
        if(direction) {
            return (start+1) % mvc.getNumPlayers();
        } else {
            start--;
            if(start < 0) {
                return mvc.getNumPlayers() - 1;
            }
        }
        return start;
    }

    private void showMode() {
        switch(mode) {
            case wonder: showWonder();
                break;
            case summary: showSummary();
                break;
            case handtrade: showHand();
                break;
            default:
                break;
        }
    }

    public void setupForTurn() {
        mvc.getActivity().findViewById(R.id.hand).setVisibility(View.VISIBLE);
        ((Button)mvc.getActivity().findViewById(R.id.hand)).setText("Hand");
    }

    public void setupForView() {
        if(playerViewing == getPlayerDirection(playerTurn, true) || playerViewing == getPlayerDirection(playerTurn, false)) {
            mvc.getActivity().findViewById(R.id.hand).setVisibility(View.VISIBLE);
            ((Button)mvc.getActivity().findViewById(R.id.hand)).setText("Trade");
        } else
        mvc.getActivity().findViewById(R.id.hand).setVisibility(View.GONE);
    }

    public void showWonder() {
        mode = Mode.wonder;
        if(playerTurn == playerViewing) {
            setupForTurn();
        } else {
            setupForView();
        }
        mvc.getActivity().findViewById(R.id.wonder).setEnabled(false);
        mvc.getActivity().findViewById(R.id.summary).setEnabled(true);
        mvc.getActivity().findViewById(R.id.hand).setEnabled(true);

        Player player = mvc.getPlayer(playerViewing);
        ((TextView) mvc.getActivity().findViewById(R.id.title)).setText(player.getWonder().getNameString());

        LinearLayout content = (LinearLayout) mvc.getActivity().findViewById(R.id.content);
        content.removeAllViews();

        CardCollection cc = new CardCollection();
        cc.addAll(player.getWonderStages());
        cc.addAll(player.getPlayedCards());
        cc.sort();
        for(Card card : cc) {
            CardView cv = new CardView(card, mvc.getActivity(), false);
            content.addView(cv);
        }
    }

    public void showSummary() {
        mode = Mode.summary;
        if(playerTurn == playerViewing) {
            setupForTurn();
        } else {
            setupForView();
        }
        mvc.getActivity().findViewById(R.id.wonder).setEnabled(true);
        mvc.getActivity().findViewById(R.id.summary).setEnabled(false);
        mvc.getActivity().findViewById(R.id.hand).setEnabled(true);

        Player player = mvc.getPlayer(playerViewing);
        ((TextView) mvc.getActivity().findViewById(R.id.title)).setText(player.getWonder().getNameString());

        LinearLayout content = (LinearLayout) mvc.getActivity().findViewById(R.id.content);
        content.removeAllViews();

        TextView tv = new TextView(mvc.getActivity());
        content.addView(tv);
        tv.setText(player.getSummary());
    }

    public void showHand() {
        if(playerViewing != getPlayerDirection(playerTurn, true) && playerViewing != getPlayerDirection(playerTurn, false)
                && playerViewing != playerTurn) {
            showSummary();
            mode = Mode.handtrade;
            return;
        }
        mode = Mode.handtrade;
        if(playerTurn == playerViewing) {
            setupForTurn();
        } else {
            setupForView();
        }
        mvc.getActivity().findViewById(R.id.wonder).setEnabled(true);
        mvc.getActivity().findViewById(R.id.summary).setEnabled(true);
        mvc.getActivity().findViewById(R.id.hand).setEnabled(false);

        Player player = mvc.getPlayer(playerViewing);
        ((TextView) mvc.getActivity().findViewById(R.id.title)).setText(player.getWonder().getNameString());

        LinearLayout content = (LinearLayout) mvc.getActivity().findViewById(R.id.content);
        if(playerTurn == playerViewing) {
            content.removeAllViews();
            Hand hand = player.getHand();
            for (Card card : hand) {
                CardView cv = new CardView(card, mvc.getActivity(), true);
                content.addView(cv);
            }
        } else {
            if(playerViewing == getPlayerDirection(playerTurn, true))
                tradeController.trade(content, true);
            else
                tradeController.trade(content, false);
        }
    }

    public void onComplete() {
        showMode();
    }

}
