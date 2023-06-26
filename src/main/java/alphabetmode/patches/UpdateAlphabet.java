package alphabetmode.patches;

import basemod.helpers.CardModifierManager;
import com.evacipated.cardcrawl.modthespire.Loader;
import com.evacipated.cardcrawl.modthespire.ModInfo;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireRawPatch;
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

public class UpdateAlphabet
{
    @SpirePatch(clz= CardCrawlGame.class,method=SpirePatch.CONSTRUCTOR)
    public static class Updater
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
                            return c.getDeclaredMethod("use", new CtClass[]{pool.get(AbstractPlayer.class.getName()), pool.get(AbstractMonster.class.getName())});
                        } catch (NotFoundException e)
                        {
                            return null;
                        }
                    }).filter(m -> m != null && !m.isEmpty() && !Modifier.isNative(m.getModifiers())).forEach(m ->
                    {
                        try
                        {
                            if (!m.getReturnType().equals(CtClass.voidType)) return;
                            m.insertAfter("{ alphabetmode.patches.UpdateAlphabet.updateBiggestChar(this);}");
                        } catch (CannotCompileException | NotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }

    public static void updateBiggestChar(AbstractCard c)
    {
        EnforceAlphabet.setBiggestUsedCard(CardModifierManager.onRenderTitle(c, c.name).toLowerCase());
    }
}
