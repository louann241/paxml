/**
 * This file is part of PaxmlTestNG.
 *
 * PaxmlTestNG is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PaxmlTestNG is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with PaxmlTestNG.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.paxml.testng;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.paxml.core.Context;
import org.paxml.launch.LaunchModel;
import org.paxml.launch.LaunchPoint;
import org.paxml.launch.Matcher;
import org.paxml.launch.Paxml;
import org.paxml.tag.AbstractTag;
import org.paxml.testng.AbstractPaxmlTestResult.ResultType;
import org.testng.annotations.Factory;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

/**
 * paxml test case factory for TestNG.
 * 
 * @author Xuetao Niu
 * 
 */
public class PaxmlTestCaseFactory {
    private static final AtomicInteger SEQUENCE = new AtomicInteger(0);

    /**
     * The paxml launch plan file path.
     */
    public static final String PARAM_NAME_PLANFILE = "paxmlTestPlanFile";
    public static final String PARAM_NAME_SUPPRESS_GROUPS = "paxmlSuppressGroups";
    public static final String PARAM_NAME_RESULT_DIR = "paxmlTestResultDir";
    public static final String PARAM_NAME_RESULT_TYPE = "paxmlTestResultType";

    private static final Log log = LogFactory.getLog(PaxmlTestCaseFactory.class);
    private static final Object LOCK = new Object();

    public static interface ILockedOperation<T> {
        T perform();
    }

    /**
     * The factory method.
     * 
     * @param planFile
     *            the plan file
     * @param suppressedGroups
     *            the execution groups to suppress
     * @param outputDir
     *            the dir to output results
     * @param resultType
     *            the format of results
     * @return the test objects.
     */
    @Factory
    @Parameters({PARAM_NAME_PLANFILE, PARAM_NAME_SUPPRESS_GROUPS, PARAM_NAME_RESULT_DIR, PARAM_NAME_RESULT_TYPE})
    public Object[] create(final String planFile, @Optional("") final String suppressedGroups,
            @Optional("./target/surefire-reports/paxml/results") final String outputDir,
            @Optional("JSON") final String resultType) {
        final List<Matcher> suppression = new ArrayList<Matcher>(0);
        for (String groupName : AbstractTag.parseDelimitedString(suppressedGroups, null)) {
            Matcher matcher = new Matcher();
            matcher.setMatchPath(false);
            matcher.setPattern(groupName);
            suppression.add(matcher);
        }
        return performLocked(new ILockedOperation<Object[]>() {

            @Override
            public Object[] perform() {

                File resultFolder = null;
                ResultType rt = null;
                final long start = System.currentTimeMillis();
                try {
                    File dir = new File(outputDir);
                    rt = ResultType.valueOf(resultType.toUpperCase());
                    resultFolder = new File(dir, SEQUENCE.getAndIncrement() + "/");
                    Object[] cases = doCreate(planFile, suppression.isEmpty() ? null : suppression, resultFolder, rt);
                    PaxmlTestCase.init(cases.length, start, FilenameUtils.getBaseName(planFile));

                    // clean the paxml thread context and log into the default
                    // file
                    Context.cleanCurrentThreadContext();
                    if (log.isInfoEnabled()) {
                        log.info("Launching totally " + cases.length + " tests ...");
                    }

                    return cases;
                } catch (Throwable e) {
                    if (log.isErrorEnabled()) {
                        log.error("Cannot create test cases", e);
                    }
                    return new Object[] {new PaxmlPlanFileFailure(e, planFile, resultFolder, rt,
                            Context.getCurrentContext(), Thread.currentThread().getName(), start,
                            System.currentTimeMillis())};
                }
            }

        });
    }

    /**
     * Let the shared context be propagated to other threads.
     * 
     * @param <T>
     * @param op
     * @return
     */
    public static <T> T performLocked(ILockedOperation<T> op) {
        synchronized (LOCK) {
            return op.perform();
        }
    }

    private Object[] doCreate(String planFile, List<Matcher> suppression, File outputDir, ResultType resultType) {

        LaunchModel model = Paxml.executePlanFile(planFile, null);

        List<LaunchPoint> points = model.getLaunchPoints(false);

        List<Object> result = new LinkedList<Object>();
        for (LaunchPoint p : points) {
            if (isSuppressed(suppression, p.getGroup())) {
                if (log.isInfoEnabled()) {
                    log.info("This scenario '" + p.getResource().getName()
                            + "' will not run because its group is suppressed: " + p.getGroup());
                }
            } else {
                result.add(new PaxmlTestCase(p, outputDir, resultType));
            }

        }
        if (result.isEmpty()) {

            if (log.isWarnEnabled()) {
                log.warn("No scenarios will run from plan file:" + planFile);
            }
        }
        return result.toArray(new Object[result.size()]);

    }

    private boolean isSuppressed(List<Matcher> suppression, String groupName) {
        if (suppression == null || suppression.isEmpty()) {
            return false;
        }
        for (Matcher m : suppression) {
            if (m.match(groupName)) {
                return true;
            }
        }
        return false;
    }
}