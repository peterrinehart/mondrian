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

package mondrian.rolap;

import mondrian.olap.Exp;
import mondrian.olap.FunDef;
import mondrian.olap.NativeEvaluator;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Composite of {@link RolapNative}s. Uses chain of responsibility
 * to select the appropriate {@link RolapNative} evaluator.
 */
public class RolapNativeRegistry extends RolapNative {

    private Map<String, RolapNative> nativeEvaluatorMap =
        new HashMap<String, RolapNative>();

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();

    public RolapNativeRegistry() {
        super.setEnabled(true);
        // Mondrian functions which might be evaluated natively.
        register("NonEmptyCrossJoin".toUpperCase(), new RolapNativeCrossJoin());
        register("CrossJoin".toUpperCase(), new RolapNativeCrossJoin());
        register("TopCount".toUpperCase(), new RolapNativeTopCount());
        register("Filter".toUpperCase(), new RolapNativeFilter());
    }

    /**
     * Returns the matching NativeEvaluator or null if <code>fun</code> can not
     * be executed in SQL for the given context and arguments.
     */
    public NativeEvaluator createEvaluator(
        RolapEvaluator evaluator, FunDef fun, Exp[] args)
    {
        if (!isEnabled()) {
            return null;
        }

        RolapNative rn = null;
        readLock.lock();
        try {
            rn = nativeEvaluatorMap.get(fun.getName().toUpperCase());
        } finally {
            readLock.unlock();
        }

        if (rn == null) {
            return null;
        }

        NativeEvaluator ne = rn.createEvaluator(evaluator, fun, args);

        if (ne != null) {
            if (listener != null) {
                NativeEvent e = new NativeEvent(this, ne);
                listener.foundEvaluator(e);
            }
        }
        return ne;
    }

    public void register(String funName, RolapNative rn) {
        writeLock.lock();
        try {
            nativeEvaluatorMap.put(funName, rn);
        } finally {
            writeLock.unlock();
        }
    }

    /** for testing */
    void setListener(Listener listener) {
        super.setListener(listener);
        readLock.lock();
        try {
            for (RolapNative rn : nativeEvaluatorMap.values()) {
                rn.setListener(listener);
            }
        } finally {
            readLock.unlock();
        }
    }

    /** for testing */
    void useHardCache(boolean hard) {
        readLock.lock();
        try {
            for (RolapNative rn : nativeEvaluatorMap.values()) {
                rn.useHardCache(hard);
            }
        } finally {
            readLock.unlock();
        }
    }

    void flushAllNativeSetCache() {
        readLock.lock();
        try {
            for (String key : nativeEvaluatorMap.keySet()) {
                RolapNative currentRolapNative = nativeEvaluatorMap.get(key);
                if (currentRolapNative instanceof RolapNativeSet
                        && currentRolapNative != null)
                {
                    RolapNativeSet currentRolapNativeSet =
                            (RolapNativeSet) currentRolapNative;
                    currentRolapNativeSet.flushCache();
                }
            }
        } finally {
            readLock.unlock();
        }
    }
}

// End RolapNativeRegistry.java
