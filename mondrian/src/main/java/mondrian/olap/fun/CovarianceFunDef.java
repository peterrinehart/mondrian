/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package mondrian.olap.fun;

import mondrian.calc.*;
import mondrian.calc.impl.AbstractDoubleCalc;
import mondrian.calc.impl.ValueCalc;
import mondrian.mdx.ResolvedFunCall;
import mondrian.olap.*;

/**
 * Definition of the <code>Covariance</code> and
 * <code>CovarianceN</code> MDX functions.
 *
 * @author jhyde
 * @since Mar 23, 2006
 */
class CovarianceFunDef extends FunDefBase {
    static final ReflectiveMultiResolver CovarianceResolver =
        new ReflectiveMultiResolver(
            "Covariance",
            "Covariance(<Set>, <Numeric Expression>[, <Numeric Expression>])",
            "Returns the covariance of two series evaluated over a set (biased).",
            new String[]{"fnxn", "fnxnn"},
            CovarianceFunDef.class);

    static final MultiResolver CovarianceNResolver =
        new ReflectiveMultiResolver(
            "CovarianceN",
            "CovarianceN(<Set>, <Numeric Expression>[, <Numeric Expression>])",
            "Returns the covariance of two series evaluated over a set (unbiased).",
            new String[]{"fnxn", "fnxnn"},
            CovarianceFunDef.class);

    private final boolean biased;

    public CovarianceFunDef(FunDef dummyFunDef) {
        super(dummyFunDef);
        this.biased = dummyFunDef.getName().equals("Covariance");
    }

    public Calc compileCall(ResolvedFunCall call, ExpCompiler compiler) {
        final ListCalc listCalc =
            compiler.compileList(call.getArg(0));
        final Calc calc1 =
            compiler.compileScalar(call.getArg(1), true);
        final Calc calc2 =
            call.getArgCount() > 2
            ? compiler.compileScalar(call.getArg(2), true)
            : new ValueCalc(call);
        return new AbstractDoubleCalc(call, new Calc[] {listCalc, calc1, calc2})
        {
            public double evaluateDouble(Evaluator evaluator) {
                TupleList memberList = listCalc.evaluateList(evaluator);
                final int savepoint = evaluator.savepoint();
                try {
                    evaluator.setNonEmpty(false);
                    final double covariance =
                        (Double) covariance(
                            evaluator,
                            memberList,
                            calc1,
                            calc2,
                            biased);
                    return covariance;
                } finally {
                    evaluator.restore(savepoint);
                }
            }

            public boolean dependsOn(Hierarchy hierarchy) {
                return anyDependsButFirst(getCalcs(), hierarchy);
            }
        };
    }
}

// End CovarianceFunDef.java
