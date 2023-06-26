package alphabetmode.patches;

import basemod.interfaces.PostBattleSubscriber;
import basemod.interfaces.PostDeathSubscriber;
import basemod.interfaces.StartGameSubscriber;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.actions.GameActionManager;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

public class ResetAlphabet implements PostDeathSubscriber, PostBattleSubscriber, StartGameSubscriber
{
    @Override
    public void receivePostBattle(AbstractRoom abstractRoom)
    {
        EnforceAlphabet.setBiggestUsedCard("a");
    }

    @Override
    public void receivePostDeath()
    {
        EnforceAlphabet.setBiggestUsedCard("a");
    }

    @Override
    public void receiveStartGame() {
        EnforceAlphabet.setBiggestUsedCard("a");
    }

    @SpirePatch(clz= GameActionManager.class,method="endTurn")
    public static class EndTurnHook
    {
        @SpirePrefixPatch
        public static void Prefix(GameActionManager __instance)
        {
            EnforceAlphabet.setBiggestUsedCard("a");
        }
    }
}
