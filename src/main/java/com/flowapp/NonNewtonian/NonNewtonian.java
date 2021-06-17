package com.flowapp.NonNewtonian;

import com.flowapp.NonNewtonian.Models.FlowLine;
import com.flowapp.NonNewtonian.Models.FlowResult;
import com.flowapp.NonNewtonian.Models.ProblemResult;
import com.flowapp.NonNewtonian.Utils.Constants;
import com.flowapp.NonNewtonian.Utils.FileUtils;
import com.flowapp.NonNewtonian.Utils.TableList;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class NonNewtonian {

    private StringBuilder steps;

    public ProblemResult nonNewtonian(float spGr,
                                      float kDash,
                                      float nDash,
                                      float anDash,
                                      float bnDash,
                                      float viscosityCp,
                                      float maxPressureBar,
                                      float iDmm,
                                      float loopIDmm,
                                      float lengthM,
                                      float flowRateM3H,
                                      float increasedFlowRateM3H,
                                      float staticHead,
                                      float nPSH,
                                      Float loopFlowRate) {
        clear();



        final float density = spGr * Constants.WaterDensityKgM3;
        final float iDm = iDmm / Constants.MmInMeter;
        final float loopIDm = iDmm / Constants.MmInMeter;

        println("Non newtonian direct:");
        final var nonDirect = calculateNonNewtonian(spGr, kDash, nDash, anDash, bnDash, maxPressureBar, iDmm, loopIDmm, lengthM, flowRateM3H, increasedFlowRateM3H, 0, staticHead, nPSH, loopFlowRate, density, iDm, loopIDm, false);
        println("Non newtonian reverse:");
        final var nonReverse = calculateNonNewtonian(spGr, kDash, nDash, anDash, bnDash, maxPressureBar, iDmm, loopIDmm, lengthM, flowRateM3H, increasedFlowRateM3H, staticHead, 0, nPSH, loopFlowRate, density, iDm, loopIDm, true);
        println("Newtonian direct:");
        final var newtDirect = calculateNewtonian(spGr, viscosityCp, maxPressureBar, iDmm, loopIDmm, lengthM, flowRateM3H, increasedFlowRateM3H, 0, staticHead, nPSH, loopFlowRate, density, iDm, loopIDm, false);
        println("Newtonian reverse:");
        final var newtReverse = calculateNewtonian(spGr, viscosityCp, maxPressureBar, iDmm, loopIDmm, lengthM, flowRateM3H, increasedFlowRateM3H, staticHead, 0, nPSH, loopFlowRate, density, iDm, loopIDm, true);

        return new ProblemResult(nonDirect, reverseLines(nonReverse, lengthM), newtDirect, reverseLines(newtReverse, lengthM), steps.toString());
    }

    private FlowResult calculateNewtonian(float spGr, float viscosityCp, float maxPressureBar, float iDmm, float loopIDmm, float lengthM, float flowRateM3H, float increasedFlowRateM3H, float startStaticHead, float endStaticHead, float nPSH, Float loopFlowRate, float density, float iDm, float loopIDm, boolean isReverse) {
        final List<FlowLine> beforeLines = calculateLines(spGr, viscosityCp, maxPressureBar, 0,lengthM, flowRateM3H, startStaticHead,endStaticHead, nPSH, density, iDm);

        println("After Increase Using booster stations");

        final List<FlowLine> linesToBoost = new ArrayList<>();

        if (beforeLines.size() > 1) {
            linesToBoost.add(beforeLines.get(0));
        }
        linesToBoost.add(beforeLines.get(beforeLines.size() - 1));
        Collections.reverse(linesToBoost);

        final List<FlowLine> afterLines = new ArrayList<>();

        for (int i = linesToBoost.size()-1; i >= 0; i--) {
            final var lineToBoost = linesToBoost.get(i);
            if (linesToBoost.size() > 1) {
                println("For pump #{}", beforeLines.size()-i);
            }
            final var newLines = calculateLines(spGr, viscosityCp, maxPressureBar,lineToBoost.getStartLength(), lineToBoost.getLength(), increasedFlowRateM3H, lineToBoost.getStartStatic(), lineToBoost.getEndStatic(), lineToBoost.getNPSH(), density, lineToBoost.getIDmm() / Constants.MmInMeter);
            afterLines.addAll(newLines);
        }

        if (loopFlowRate == null && loopIDmm == iDmm) {
            loopFlowRate = increasedFlowRateM3H / 2.0f;
        }

        println("Increasing using a loop:");
        final List<FlowLine> loops = new ArrayList<>();
        final List<FlowLine> linesToLoop = new ArrayList<>();
        //final List<FlowLine> complementaryLines = new ArrayList<>();

        if (beforeLines.size() > 1) {
            linesToLoop.add(beforeLines.get(0));
        }
        linesToLoop.add(beforeLines.get(beforeLines.size() - 1));
        Collections.reverse(linesToLoop);
        for (int i = linesToLoop.size() - 1; i >= 0; i--) {
            final var lineToLoop = linesToLoop.get(i);
            if (linesToLoop.size() > 1) {
                println("For pump #{}", beforeLines.size()-i);
            }
            final float loopVelocity = calculateV(loopFlowRate, loopIDm);
            final float loopNre = calculateNre(viscosityCp, density, loopIDm, loopVelocity);
            final float loopF = calculateF(loopNre);

            final float lineVelocity = calculateV(increasedFlowRateM3H, iDm);
            final float lineNre = calculateNre(viscosityCp, density, iDm, lineVelocity);
            final float lineF = calculateF(lineNre);

            final List<Object[]> steps = new ArrayList<>();
            steps.add(new Object[]{ "", "Line", "Loop"});
            steps.add(new Object[]{ "I.D", iDm, loopIDm});
            steps.add(new Object[]{ "Q", increasedFlowRateM3H, loopFlowRate});
            steps.add(new Object[]{ "Nre", String.format("%.6f", lineNre), String.format("%.6f",loopNre)});
            steps.add(new Object[]{ "F", String.format("%.6f", lineF), String.format("%.6f",loopF)});
            renderTable(steps);

            final float lineStaticHead = lineToLoop.getEndStatic() - lineToLoop.getStartStatic();
            final float c = (float) (lineF * Math.pow(lineVelocity, 2) / (2 * 9.8f * iDm));
            final float e = (float) (loopF * Math.pow(loopVelocity, 2) / (2 * 9.8f * loopIDm));
            final float hfBefore = lineToLoop.getStartHt() - lineToLoop.getNPSH() - lineStaticHead;
            final float loopLength = (hfBefore - c * lineToLoop.getLength()) / (e-c);

            println("hf(Before) = F(L-LoopL)V^2/(19.6 * IDLine) + FLoopLV^2/(19.6 * IDLoop)");
            println("{} = {} ({} - LoopL) + {} LoopL", hfBefore, c, lineToLoop.getLength(), e);
            println("LoopL = {} m", loopLength);

            if (loopLength > lineToLoop.getLength()) {
                println("Since Loop length > Line length, then an increased flow rate using a loop is not feasible");
                loops.clear();
                break;
            } else {
                final float loopStaticHead = lineStaticHead * loopLength / lineToLoop.getLength();
                final float loopEndStaticHead = lineToLoop.getEndStatic();
                final float loopStartStaticHead = loopEndStaticHead - loopStaticHead;
                final float loopStartLength = lineToLoop.getStartLength() + lineToLoop.getLength() - loopLength;
                final float loopHf = loopLength * e;
                final var loop = new FlowLine(loopStartStaticHead, loopHf + loopStaticHead + lineToLoop.getNPSH(), loopFlowRate, loopVelocity, loopIDm * Constants.MmInMeter, loopStartLength, loopLength, loopEndStaticHead, lineToLoop.getNPSH(), loopNre, loopF);
                loops.add(loop);
            }
        }
        return new FlowResult(beforeLines, afterLines, loops);
    }

    private FlowResult calculateNonNewtonian(float spGr, float kDash, float nDash, float anDash, float bnDash, float maxPressureBar, float iDmm, float loopIDmm, float lengthM, float flowRateM3H, float increasedFlowRateM3H, float startStaticHead, float endStaticHead, float nPSH, Float loopFlowRate, float density, float iDm, float loopIDm, boolean isReverse) {
        final List<FlowLine> beforeLines = calculateLines(spGr, kDash, nDash, anDash, bnDash, maxPressureBar, 0,lengthM, flowRateM3H, startStaticHead,endStaticHead, nPSH, density, iDm);

        println("After Increase Using booster stations");

        final List<FlowLine> linesToBoost = new ArrayList<>();

        if (beforeLines.size() > 1) {
            linesToBoost.add(beforeLines.get(0));
        }
        linesToBoost.add(beforeLines.get(beforeLines.size() - 1));
        Collections.reverse(linesToBoost);

        final List<FlowLine> afterLines = new ArrayList<>();

        for (int i = linesToBoost.size()-1; i >= 0; i--) {
            final var lineToBoost = linesToBoost.get(i);
            if (linesToBoost.size() > 1) {
                println("For pump #{}", beforeLines.size()-i);
            }
            final var newLines = calculateLines(spGr, kDash, nDash, anDash, bnDash, maxPressureBar,lineToBoost.getStartLength(), lineToBoost.getLength(), increasedFlowRateM3H, lineToBoost.getStartStatic(), lineToBoost.getEndStatic(), lineToBoost.getNPSH(), density, lineToBoost.getIDmm() / Constants.MmInMeter);
            afterLines.addAll(newLines);
        }

        if (loopFlowRate == null && loopIDmm == iDmm) {
            loopFlowRate = increasedFlowRateM3H / 2.0f;
        }

        println("Increasing using a loop:");
        final List<FlowLine> loops = new ArrayList<>();
        final List<FlowLine> linesToLoop = new ArrayList<>();
        //final List<FlowLine> complementaryLines = new ArrayList<>();

        if (beforeLines.size() > 1) {
            linesToLoop.add(beforeLines.get(0));
        }
        linesToLoop.add(beforeLines.get(beforeLines.size() - 1));
        Collections.reverse(linesToLoop);
        for (int i = linesToLoop.size() - 1; i >= 0; i--) {
            final var lineToLoop = linesToLoop.get(i);
            if (linesToLoop.size() > 1) {
                println("For pump #{}", beforeLines.size()-i);
            }

            final float loopVelocity = calculateV(loopFlowRate, loopIDm);
            final float loopNre = calculateNre(kDash, nDash, density, loopIDm, loopVelocity);
            final float loopF = calculateF(anDash, bnDash, loopNre);

            final float lineVelocity = calculateV(increasedFlowRateM3H, iDm);
            final float lineNre = calculateNre(kDash, nDash, density, iDm, lineVelocity);
            final float lineF = calculateF(anDash, bnDash, lineNre);

            final List<Object[]> steps = new ArrayList<>();
            steps.add(new Object[]{ "", "Line", "Loop"});
            steps.add(new Object[]{ "I.D", iDm, loopIDm});
            steps.add(new Object[]{ "Q", increasedFlowRateM3H, loopFlowRate});
            steps.add(new Object[]{ "Nre", String.format("%.6f", lineNre), String.format("%.6f",loopNre)});
            steps.add(new Object[]{ "F", String.format("%.6f", lineF), String.format("%.6f",loopF)});
            renderTable(steps);

            final float lineStaticHead = lineToLoop.getEndStatic() - lineToLoop.getStartStatic();
            final float c = (float) (lineF * Math.pow(lineVelocity, 2) / (2 * 9.8f * iDm));
            final float e = (float) (loopF * Math.pow(loopVelocity, 2) / (2 * 9.8f * loopIDm));
            final float hfBefore = lineToLoop.getStartHt() - lineToLoop.getNPSH() - lineStaticHead;
            final float loopLength = (hfBefore - c * lineToLoop.getLength()) / (e-c);

            println("hf(Before) = F(L-LoopL)V^2/(19.6 * IDLine) + FLoopLV^2/(19.6 * IDLoop)");
            println("{} = {} ({} - LoopL) + {} LoopL", hfBefore, c, lineToLoop.getLength(), e);
            println("LoopL = {} m", loopLength);

            if (loopLength > lineToLoop.getLength()) {
                println("Since Loop length > Line length, then an increased flow rate using a loop is not feasible");
                loops.clear();
                break;
            } else {
                final float loopStaticHead = lineStaticHead * loopLength / lineToLoop.getLength();
                final float loopEndStaticHead = lineToLoop.getEndStatic();
                final float loopStartStaticHead = loopEndStaticHead - loopStaticHead;
                final float loopStartLength = lineToLoop.getStartLength() + lineToLoop.getLength() - loopLength;
                final float loopHf = loopLength * e;
                final var loop = new FlowLine(loopStartStaticHead, loopHf + loopStaticHead + lineToLoop.getNPSH(), loopFlowRate, loopVelocity, loopIDm * Constants.MmInMeter, loopStartLength, loopLength, loopEndStaticHead, lineToLoop.getNPSH(), loopNre, loopF);
                loops.add(loop);
            }
        }
        return new FlowResult(beforeLines, afterLines, loops);
    }

    private FlowResult reverseLines(FlowResult flow, float lengthM) {
        return new FlowResult(reverse(flow.getBefore(), lengthM), reverse(flow.getAfter(), lengthM),reverse(flow.getLoops(), lengthM));
    }

    private List<FlowLine> reverse(List<FlowLine> lines, float lengthM) {
        for (int i = 0; i < lines.size(); i++) {
            final var oldLine = lines.get(i);
            final var newLine = new FlowLine(oldLine.getStartStatic(), oldLine.getStartHt(), oldLine.getQ(), oldLine.getV(), oldLine.getIDmm(), lengthM - oldLine.getStartLength() - oldLine.getLength(),oldLine.getLength(), oldLine.getEndStatic(), oldLine.getNPSH(), oldLine.getNre(), oldLine.getF());
            lines.set(i, newLine);
        }
        Collections.reverse(lines);
        return lines;
    }

    private List<FlowLine> calculateLines(float spGr, float viscosityCp, float maxPressureBar, float startLength,float lengthM, float flowRateM3H, float startStaticHead, float endStaticHead, float nPSH, float density, float iDm) {
        final float v = calculateV(flowRateM3H, iDm);
        println("v = {} m/s", v);
        final float nre = calculateNre(viscosityCp, density, iDm, v);
        println("Nre = vdρ/μ = {} * {} * {} / {} = {}", v, iDm, density, viscosityCp, nre);
        final float f = calculateF(nre);
        println("f = {}", f);
        final float hf = calculateHf(lengthM, iDm, v, f);
        println("hf = {} m", hf);
        final float ht = hf + (endStaticHead - startStaticHead) + nPSH;
        println("ht = {} + {} + {} = {} m", hf, endStaticHead - startStaticHead, nPSH, ht);
        final float p = ht * spGr / 10.0f;
        println("P = ht * Sp.Gr / 10 = {} * {} / 10 = {} bar", ht, spGr, p);
        final float numOfPumpsF = p / maxPressureBar;
        final int numOfPumps = (int) Math.ceil(numOfPumpsF);
        println("Pumps = {}/{} = {} = {}", p, maxPressureBar, numOfPumpsF, numOfPumps);

        final List<FlowLine> lines = new ArrayList<>();

        if (numOfPumps > 1) {
            float totalLength = startLength;
            float lastStaticHead = startStaticHead;
            for (int i = 0; i < numOfPumps; i++) {
                final float linePressure;
                final boolean isLast = i+1 > numOfPumpsF;
                if (isLast) {
                    linePressure = (1-(numOfPumps - numOfPumpsF)) * maxPressureBar;
                } else {
                    linePressure = maxPressureBar;
                }
                final float lineHt = linePressure * 10 / spGr;
                final float length = linePressure / p * lengthM;
                final float lineStartStaticHead = lastStaticHead;
                lastStaticHead += (endStaticHead - startStaticHead) * length / lengthM;

                final float lineStart = totalLength;
                totalLength += length;

                final var line = new FlowLine(lineStartStaticHead, lineHt, flowRateM3H, v,iDm * Constants.MmInMeter, lineStart, length, lastStaticHead, nPSH, nre, f);
                lines.add(line);
            }
        } else {
            final var line = new FlowLine(startStaticHead, ht, flowRateM3H, v,iDm * Constants.MmInMeter, startLength, lengthM, endStaticHead, nPSH, nre, f);
            lines.add(line);
        }

        renderLines(lines);

        return lines;
    }

    private List<FlowLine> calculateLines(float spGr, float kDash, float nDash, float anDash, float bnDash, float maxPressureBar, float startLength,float lengthM, float flowRateM3H, float startStaticHead, float endStaticHead, float nPSH, float density, float iDm) {

        final float v = calculateV(flowRateM3H, iDm);
        println("v = 4*Q/(π*ID^2*3600) = 4*{}/(π*{}^2*3600) = {}", flowRateM3H, iDm, v);
        final float nre = calculateNre(kDash, nDash, density, iDm, v);
        println("Nre,me = ID^n' * v^(2-n') * ρ / (k' * 8^(n'-1)) = {}^{} * {}^(2-{}) * {} / ({} * 8^({}-1)) = {}", iDm, nDash, v, nDash, density, kDash, nDash, nre);
        final float f = calculateF(anDash, bnDash, nre);
        println("Since Nre > Nrec, then Turbulent");
        println("f = an'/(Nre,me^bn') = {}/({}^{}) = {}", anDash, nre, bnDash, f);
        final float hf = calculateHf(lengthM, iDm, v, f);
        println("hf = flv^2/(2*g*ID) = {} * {} * {}^2 / ({} * {}) = {}", f, lengthM, v, 2*9.8f, iDm, hf);
        final float ht = hf + (endStaticHead - startStaticHead) + nPSH;
        println("ht = {} + {} + {} = {}", hf, (endStaticHead - startStaticHead), nPSH, ht);
        final float p = ht * spGr / 10.0f;
        println("P = ht * Sp.Gr / 10 = {} * {} / 10 = {} bar", ht, spGr, p);
        final float numOfPumpsF = p / maxPressureBar;
        final int numOfPumps = (int) Math.ceil(numOfPumpsF);
        println("Pumps = {}/{} = {} = {}", p, maxPressureBar, numOfPumpsF, numOfPumps);

        final List<FlowLine> lines = new ArrayList<>();

        if (numOfPumps > 1) {
            float totalLength = startLength;
            float lastStaticHead = startStaticHead;
            for (int i = 0; i < numOfPumps; i++) {
                final float linePressure;
                final boolean isLast = i+1 > numOfPumpsF;
                if (isLast) {
                    linePressure = (1-(numOfPumps - numOfPumpsF)) * maxPressureBar;
                } else {
                    linePressure = maxPressureBar;
                }
                final float lineHt = linePressure * 10 / spGr;
                final float length = linePressure / p * lengthM;
                final float lineStartStaticHead = lastStaticHead;
                lastStaticHead += (endStaticHead - startStaticHead) * length / lengthM;

                final float lineStart = totalLength;
                totalLength += length;

                final var line = new FlowLine(lineStartStaticHead, lineHt, flowRateM3H, v,iDm * Constants.MmInMeter, lineStart, length, lastStaticHead, nPSH, nre, f);
                lines.add(line);
            }
        } else {
            final var line = new FlowLine(startStaticHead, ht, flowRateM3H, v,iDm * Constants.MmInMeter, startLength, lengthM, endStaticHead, nPSH, nre, f);
            lines.add(line);
        }

        renderLines(lines);

        return lines;
    }

    private float calculateHf(float lengthM, float iDm, float v, float f) {
        return (float) (f * lengthM * Math.pow(v, 2) / (2 * 9.8 * iDm));
    }

    private float calculateF(float nRe) {
        final float f;
        if (nRe <= Constants.LaminarFlowMaxNre) {
            f = 64 / nRe;
        } else if (nRe < Constants.TurbulentFlowMaxNre) {
            f = (float) (0.5f / Math.pow(nRe, 0.3f));
        } else {
            f = (float) (0.316 / Math.pow(nRe, 0.25));
        }
        return f;
    }

    private float calculateF(float anDash, float bnDash, float nre) {
        final float f;
        if (nre != 0) {
            f = (float) (anDash / Math.pow(nre, bnDash));
        } else {
            f = 0;
        }
        return f;
    }

    private float calculateNre(float kDash, float nDash, float density, float iDm, float v) {
        return (float) (Math.pow(iDm, nDash) * Math.pow(v, 2 - nDash) * density / (kDash * Math.pow(8, nDash - 1)));
    }

    private float calculateNre(float viscosityCp, float density, float iDm, float v) {
        return v * iDm * density / viscosityCp;
    }

    private float calculateV(float flowRateM3H, float iDm) {
        return (float) (4 * flowRateM3H / (Math.PI * Math.pow(iDm, 2) * 3600));
    }

    private void renderLines(List<FlowLine> beforeLines) {
        final List<Object[]> steps = new ArrayList<>();
        steps.add(new Object[]{ "Pump Station", "ht", "Start", "Length", "End"});
        for (int i = 0; i < beforeLines.size(); i++) {
            final var station = beforeLines.get(i);
            steps.add(new Object[]{ i+1, station.getStartHt(), station.getStartLength(), station.getLength(), station.getStartLength() + station.getLength()});
        }
        renderTable(steps);
    }

    private void renderTable(List<Object[]> args) {
        renderTable(args.toArray(new Object[0][0]));
    }

    private void renderTable(Object[] ... args) {
        final var temp = args[0];
        final String[] firstRow = new String[temp.length];
        for (int i = 0; i < temp.length; i++) {
            firstRow[i] = temp[i].toString();
        }
        TableList at = new TableList(firstRow).withUnicode(true);
        final var newRows = Arrays.stream(args).skip(1).map(row -> {
            final String[] newRow = new String[row.length];
            for (int i = 0; i < row.length; i++) {
                final Object object = row[i];
                if (object instanceof Number) {
                    newRow[i] = formatNumber((Number) object);
                } else {
                    newRow[i] = object.toString();
                }
            }
            return newRow;
        }).collect(Collectors.toList());
        for (var row: newRows) {
            at.addRow(row);
        }
        String rend = at.render();
        println(rend);
    }

    private void println(@NotNull String pattern, Object... args) {
        final String message = format(pattern, args);
        steps.append(message).append('\n');
        FileUtils.printOut(message);
    }

    private void clear() {
        steps = new StringBuilder();
        FileUtils.clear();
    }

    private String formatNumber(Number number) {
        final var value = number.floatValue();
        if (number instanceof Double) {
            return number.toString();
        }
        if (value < 0) {
            return String.format("%.7f", value);
        } else if (value == 0) {
            return  "0";
        } else {
            return String.format("%.4f", value).replace(".0000", "");
        }
    }

    @NotNull
    private String format(@NotNull String pattern, Object... args) {
        Pattern rePattern = Pattern.compile("\\{([0-9+-]*)}", Pattern.CASE_INSENSITIVE);
        Matcher matcher = rePattern.matcher(pattern);
        int counter = -1;
        while (matcher.find()) {
            counter++;
            String number = matcher.group(1);
            if (number == null) {
                number = "";
            }
            if (!number.isBlank()) {
                if (number.equals("+")) {
                    number = "\\+";
                    counter++;
                } else if (number.equals("-")) {
                    counter--;
                } else {
                    counter = Integer.parseInt(number);
                }
            }
            counter = clamp(counter, 0, args.length - 1);
            String toChange = "\\{" + number + "}";
            Object object = args[counter];
            String objectString;
            if (object instanceof Number) {
                objectString = formatNumber((Number) object);
            } else {
                objectString = object.toString();
            }
            String result = objectString;
            pattern = pattern.replaceFirst(toChange, result);
        }
        return pattern;
    }

    private <T extends Comparable<T>> T clamp(T val, T min, T max) {
        if (val.compareTo(min) < 0) return min;
        else if (val.compareTo(max) > 0) return max;
        else return val;
    }
}
