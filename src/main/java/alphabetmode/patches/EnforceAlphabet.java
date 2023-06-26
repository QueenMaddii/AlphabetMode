package alphabetmode.patches;

import basemod.helpers.CardModifierManager;
import com.evacipated.cardcrawl.modthespire.Loader;
import com.evacipated.cardcrawl.modthespire.ModInfo;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import javassist.*;
import org.clapper.util.classutil.*;

import java.io.File;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.util.ArrayList;

public class EnforceAlphabet
{
    @SpirePatch(clz= CardCrawlGame.class,method=SpirePatch.CONSTRUCTOR)
    public static class Enforcer
    {
        @SpireRawPatch
        public static void rawr(CtBehavior ctBehavior) throws NotFoundException, CannotCompileException {
            ClassFinder finder = new ClassFinder();
            finder.add(new File(Loader.STS_JAR));

            for (ModInfo info : Loader.MODINFOS) {
                if (info.jarURL != null) {
                    try {
                        finder.add(new File(info.jarURL.toURI()));
                    } catch (URISyntaxException e) {
                        //Oops all errors
                    }
                }
            }

            ClassPool pool = ctBehavior.getDeclaringClass().getClassPool();

            ClassFilter abstractCardFilter = new AndClassFilter(
                    new NotClassFilter(new InterfaceOnlyClassFilter()),
                    //new NotClassFilter(new AbstractClassFilter()),
                    new ClassModifiersClassFilter(Modifier.PUBLIC),
                    new OrClassFilter(
                            new SubclassClassFilter(AbstractCard.class),
                            ((classInfo, classFinder) -> {
                               return classInfo.getClassName().equals(AbstractCard.class.getName());
                            })
                    )
            );
            ArrayList<ClassInfo> abstractCardClasses = new ArrayList<>();
            finder.findClasses(abstractCardClasses, abstractCardFilter);

            abstractCardClasses.stream().map(c ->
                    {
                        try
                        {
                            return pool.get(c.getClassName());
                        } catch (NotFoundException e)
                        {
                            throw new RuntimeException(e);
                        }
                    })
                    .map(c ->
                    {
                        try
                        {
                            return c.getDeclaredMethod("canUse", new CtClass[]{pool.get(AbstractPlayer.class.getName()), pool.get(AbstractMonster.class.getName())});
                        } catch (NotFoundException e)
                        {
                            return null;
                        }
                    }).filter(m -> m != null && !m.isEmpty() && !Modifier.isNative(m.getModifiers())).forEach(m ->
                    {
                        try
                        {
                            if (!m.getReturnType().equals(CtClass.booleanType)) return;
                            m.insertAfter("{$_ = alphabetmode.patches.EnforceAlphabet.alphabetCheck($_, this);}");
                        } catch (CannotCompileException | NotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    });
//                    }).flatMap(Arrays::stream).forEach(m -> {
//                        if (m.getName().equals("canUse") && !m.isEmpty() && !Modifier.isNative(m.getModifiers()))
//                        {
//                            try {
//                                m.insertBefore("if (true) {return false;}");
//                            } catch (CannotCompileException e) {
//                                throw new RuntimeException(e);
//                            }
//                        }
//                    });
        }
    }

    @SpirePatch(clz=AbstractCard.class,method="canUse")
    public static class AbstractCardAlphabetPatch
    {
        @SpirePostfixPatch
        public static boolean Postfix(boolean __result, AbstractCard __instance, AbstractPlayer p, AbstractMonster m)
        {
            return (alphabetCheck(__result, __instance));
        }
    }

    public static void setBiggestUsedCard(String biggestUsedCard)
    {
        EnforceAlphabet.biggestUsedCard = biggestUsedCard;
    }

    private static String biggestUsedCard = "a";
    public static boolean alphabetCheck(boolean b, AbstractCard c)
    {
        return b && CardModifierManager.onRenderTitle(c, c.name).toLowerCase().compareTo(biggestUsedCard) >= 0;
    }
}