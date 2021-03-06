package com.example.imaginebaby;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StatisticsFragment extends Fragment implements DatePickerDialog.OnDateSetListener {

    private Activity mStatisticsActivity;
    private Context mStatisticsContext;

    // Statistics view components
    private Spinner mStatSpinner;
    private LineChart mLineChart;
    private TextView mStatResultText;
    private EditText mStatStartEditText;
    private EditText mStatEndEditText;

    // Spinner item's text
    private String mSpinnerText;

    // Date format
    private SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy.MM.dd", Locale.KOREA);
    // Today : Date -> String
    private String mTodayStr;
    // A week ago: Date -> String
    private String mAWeekAgoStr;
    // A month ago: Date -> String
    private String mAMonthAgoStr;

    // Date : year, month, day
    private int mYear;
    private int mMonth;
    private int mDay;
    private String mCustomStart;
    private String mCustomEnd;
    private boolean isClickStartDate;
    private boolean isClickEndDate;

    // ArrayList for units and total units in each days
    private ArrayList<Integer> sumOfDayUnitArrayList;
    private ArrayList<Integer> sumOfDayTotalUnitArrayList;

    // Save date in selected period
    private ArrayList<String> dateArrayList;

    // Accent color for drawing unit chart
    private String mColorAccentStr = "#FF5722";
    private int mColorAccentInt = Color.parseColor(mColorAccentStr);

    // Primary dark color for text
    private String mColorDarkStr = "#616161";
    private int mColorDarkInt = Color.parseColor(mColorDarkStr);

    // Primary color for drawing total unit chart
    private String mColorStr = "#9E9E9E";
    private int mColorInt = Color.parseColor(mColorStr);


    final static int WEEK_PERIOD = 1;
    final static int MONTH_PERIOD = 2;
    final static int CUSTOM_PERIOD = 3;
    private int selectedPeriod = 0;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             Bundle savedInstanceState) {
        // Set identifier
        View mStatisticsView = inflater.inflate(R.layout.fragment_statistics, container, false);
        mStatisticsActivity = getActivity();
        mStatisticsContext = mStatisticsView.getContext();


        // Get chart view
        mLineChart = mStatisticsView.findViewById(R.id.chart);

        // Get result text view
        mStatResultText = mStatisticsView.findViewById(R.id.stat_result);

        // Get period edit text view
        mStatStartEditText = mStatisticsView.findViewById(R.id.start_edit_txt);
        mStatEndEditText = mStatisticsView.findViewById(R.id.end_edit_txt);

        return mStatisticsView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    /**
     * Set data to chart view
     */
    private void addData() {
        // Unit data
        List<Entry> unitList = new ArrayList<>();

        for (int i = 0; i < sumOfDayUnitArrayList.size(); i++) {
            unitList.add(new Entry(i, sumOfDayUnitArrayList.get(i)));
        }

        // Units line data set
        LineDataSet unitDataSet = new LineDataSet(unitList, "일주일");
        customLineDataSet(unitDataSet, mColorAccentInt);

        // Total unit data
        List<Entry> totalUnitEntries = new ArrayList<>();

        for (int i = 0; i < sumOfDayTotalUnitArrayList.size(); i++) {
            totalUnitEntries.add(new Entry(i, sumOfDayTotalUnitArrayList.get(i)));
        }

        // Total units line data set
        LineDataSet totalUnitDataSet = new LineDataSet(totalUnitEntries,"이건 뭔데");
        customLineDataSet(totalUnitDataSet, mColorDarkInt);

        // Set x-axis
        XAxis xAxis = mLineChart.getXAxis();
        xAxis.setDrawGridLines(false);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularityEnabled(true);
        /*xAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                if (dateArrayList.size() > (int) value) {
                    return dateArrayList.get((int) value).replace(".", "/").substring(5);
                } else return null;
            }
        });*/
        // Customize the chart
        customChart();

        // Consist line data sets
        LineData data = new LineData();
        data.addDataSet(unitDataSet);
        data.addDataSet(totalUnitDataSet);
        data.notifyDataChanged();

        // Set data to chart view
        mLineChart.setData(data);
        mLineChart.notifyDataSetChanged();
        mLineChart.invalidate();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
    }

    /**
     * This function is branched according to selected item.
     */
    void selectedSpinnerItem() {
        mStatSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                mSpinnerText = mStatSpinner.getSelectedItem().toString();
                // ----------WEEK-----------
                if (mSpinnerText.equals("WEEK")) {
                    drawWeekPeriodStatisticsChart();
                    selectedPeriod = WEEK_PERIOD;
                }
                // -----------MONTH-----------
                else if (mSpinnerText.equals("MONTH")) {
                    drawMonthPeriodStatisticsChart();
                    selectedPeriod = MONTH_PERIOD;
                }
                // -----------CUSTOM------------
                else {
                    // initialize the chart
                    initializeChart();
                    drawCustomPeriodStatisticsChart();
                    selectedPeriod = CUSTOM_PERIOD;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    private void drawCustomPeriodStatisticsChart() {

        // Can touch edit text, but focus is disabled.
        mStatStartEditText.setClickable(true);
        mStatEndEditText.setClickable(true);

        // Click start edit text
        mStatStartEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkClick(true, false);
                showDateDialog(mCustomStart);
            }
        });

        // Click end edit text
        mStatEndEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkClick(false, true);
                showDateDialog(mCustomEnd);
            }
        });

        if (selectedPeriod == CUSTOM_PERIOD && mCustomStart != null && mCustomEnd != null) {
            getPeriod(mCustomStart, mCustomEnd);
            addData();
        }
    }

    private void drawMonthPeriodStatisticsChart() {
        // Get Date
        getToday();
        getAMonthAgo();

        // Set period to text view
        mStatStartEditText.setText(mAMonthAgoStr);
        mStatEndEditText.setText(mTodayStr);

                    /* Get sum of unit/total unit each days, whole value of these,
                       and set text result.*/
        getPeriod(mAMonthAgoStr, mTodayStr);

        // Add data
        addData();
    }

    private void drawWeekPeriodStatisticsChart() {
        // Get Date
        getToday();
        getAWeekAgo();

        // Set period to edit text
        mStatStartEditText.setText(mAWeekAgoStr);
        mStatEndEditText.setText(mTodayStr);

                    /* Get sum of unit/total unit each days, whole value of these,
                       and set text result.*/
        getPeriod(mAWeekAgoStr, mTodayStr);

        // Add data
        addData();
    }

    /**
     * Get today's date
     */
    void getToday() {
        Date mTodayDate = new Date();
        mTodayStr = mDateFormat.format(mTodayDate);
    }

    /**
     * Get a week ago's date
     */
    void getAWeekAgo() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -6);
        Date mAWeekAgoDate = calendar.getTime();
        mAWeekAgoStr = mDateFormat.format(mAWeekAgoDate);
    }

    /**
     * Get a month ago's date
     */
    void getAMonthAgo() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);
        Date mAMonthAgoDate = calendar.getTime();
        mAMonthAgoStr = mDateFormat.format(mAMonthAgoDate);
    }

    /**
     * This function gets the date corresponding to the period.
     *
     * @param startDate start date
     * @param endDate   ~ end date
     */
    void getPeriodFromSql(Date startDate, Date endDate) {
        int itemUnit = 0;
        int itemTotalUnit = 0;
        sumOfDayUnitArrayList = new ArrayList<>();
        sumOfDayTotalUnitArrayList = new ArrayList<>();
        int sumOfWholeUnits = 0;
        int sumOfWholeTotalUnits = 0;

        dateArrayList = new ArrayList<>();
        Date currentDate = startDate;
        while (currentDate.compareTo(endDate) <= 0) {
            dateArrayList.add(mDateFormat.format(currentDate));
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(currentDate);
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            currentDate = calendar.getTime();
        }

        for (String date : dateArrayList) {
            String[] dateStr = {date};

            sumOfDayUnitArrayList.add(itemUnit);
            sumOfDayTotalUnitArrayList.add(itemTotalUnit);

            // Re-initialize
            itemUnit = 0;
            itemTotalUnit = 0;
        }

        // Sum units
        for (int i : sumOfDayUnitArrayList) {
            sumOfWholeUnits += i;
        }

        // Sum total units
        for (int i : sumOfDayTotalUnitArrayList) {
            sumOfWholeTotalUnits += i;
        }

        mStatResultText.setText(getPercent(sumOfWholeTotalUnits, sumOfWholeUnits));
    }

    /**
     * This function get percent : sum of whole units in period / sum of whole total units in period
     *
     * @param sumOfWholeTotalUnits sum of whole total units in period
     * @param sumOfWholeUnits      sum of whole units in period
     * @return percent string set
     */
    String getPercent(int sumOfWholeTotalUnits, int sumOfWholeUnits) {
        double mPercent;
        if (sumOfWholeTotalUnits != 0) {
            mPercent = Math.round(((double) sumOfWholeUnits / sumOfWholeTotalUnits) * 100);
        } else {
            mPercent = 0;
        }
        return mPercent + " %  ( " + sumOfWholeUnits + " / " + sumOfWholeTotalUnits + " )";
    }

    /**
     * Set design to line data set object
     *
     * @param lineDataSet lineDataSet object
     */
    void customLineDataSet(LineDataSet lineDataSet, int color) {
        lineDataSet.setLineWidth(2f);
        lineDataSet.setValueTextSize(0);
        lineDataSet.setCircleRadius(6f);
        lineDataSet.setColor(color);
        lineDataSet.setCircleColor(color);
        lineDataSet.setHighLightColor(color);
        lineDataSet.setValueTextColor(color);
    }

    /**
     * Customizing line chart
     */
    void customChart() {
        // Set padding
        mLineChart.setExtraRightOffset(40f);
        mLineChart.setExtraBottomOffset(20f);
        // Set color
        mLineChart.setBorderColor(mColorAccentInt);
        mLineChart.setBackgroundColor(Color.WHITE);
        // Set line design
        mLineChart.setDrawGridBackground(false);
        mLineChart.getDescription().setEnabled(false);
        mLineChart.setDrawBorders(false);

        // Y - right - Axis
        mLineChart.getAxisRight().setEnabled(false);

        // X - Axis
        mLineChart.getXAxis().setYOffset(15f);
        mLineChart.getXAxis().setTextSize(11f);
        mLineChart.getXAxis().setTextColor(mColorAccentInt);
        mLineChart.getXAxis().setDrawAxisLine(false);
        mLineChart.getXAxis().setDrawGridLines(false);

        // Y - left - Axis
        mLineChart.getAxisLeft().setXOffset(15f);
        mLineChart.getAxisLeft().setTextSize(14f);
        mLineChart.getAxisLeft().setGranularity(1f);
        mLineChart.getAxisLeft().setAxisMinimum(0);
        mLineChart.getAxisLeft().setTextColor(mColorInt);
        mLineChart.getAxisLeft().setAxisLineColor(mColorInt);
        mLineChart.getAxisLeft().setDrawGridLines(false);

        // enable touch gestures
        mLineChart.setTouchEnabled(true);

        // enable scaling and dragging
        mLineChart.setDragEnabled(true);
        mLineChart.setScaleEnabled(true);

        // if disabled, scaling can be done on x- and y-axis separately
        mLineChart.setPinchZoom(false);



        // Show dynamic animation
        mLineChart.animateXY(2000, 2000);

        // Customizing Legend label design
        Legend l = mLineChart.getLegend();
        l.setXEntrySpace(20f);
        l.setTextSize(11f);
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setDrawInside(false);
    }

    /**
     * Set date to user selected date
     */
    @Override
    public void onDateSet(DatePicker view, int selectedYear, int selectedMonth, int selectedDay) {
        Calendar calendar = Calendar.getInstance();

        // Assign Selected Date in DatePickerDialog
        mYear = selectedYear;
        mMonth = selectedMonth;
        mDay = selectedDay;

        // Set Date in List
        calendar.set(mYear, mMonth, mDay);
        String mDate = mDateFormat.format(calendar.getTime());

        // Custom statistics
        if (mSpinnerText.equals("s")) {
            // Select start edit text
            if (isClickStartDate && !isClickEndDate) {
                mCustomStart = mDate;
                mStatStartEditText.setText(mDate);
            }
            // Select end edit text
            else {
                mCustomEnd = mDate;
                mStatEndEditText.setText(mDate);
            }

            // If the two edit text is not empty then get statistics in selected period.
            if (mCustomStart != null && mCustomEnd != null) {
                // Check date: start date <= end date ?
                if (checkDate(mCustomStart, mCustomEnd)) {
                    // Reinitialize
                    mCustomStart = null;
                    mCustomEnd = null;
                    initializeChart();
                    return;
                }

                /* Get sum of unit/total unit each days, whole value of these,
                       and set text result.*/
                getPeriod(mCustomStart, mCustomEnd);

                // Add data
                addData();
            }
        }
    }

    /**
     * Show date picker dialog
     *
     * @param dateStr start or end date string
     */
    void showDateDialog(@Nullable String dateStr) {
        Calendar calendar = Calendar.getInstance();

        try {
            if (dateStr != null) {
                Date date = mDateFormat.parse(dateStr);
                calendar.setTime(date);
            } else {
                calendar.setTime(new Date());
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        mYear = calendar.get(Calendar.YEAR);
        mMonth = calendar.get(Calendar.MONTH);
        mDay = calendar.get(Calendar.DAY_OF_MONTH);


    }

    /**
     * Set bool values according to clicked edit text
     *
     * @param start is clicked start edit text?
     * @param end   is clicked end edit text?
     */
    void checkClick(boolean start, boolean end) {
        isClickStartDate = start;
        isClickEndDate = end;
    }

    /**
     * Call getPeriodFromSql function.
     *
     * @param start start date string
     * @param end   end date string
     */
    void getPeriod(String start, String end) {
        try {
            getPeriodFromSql(mDateFormat.parse(start)
                    , mDateFormat.parse(end));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * Is the period invalid?
     *
     * @param start start date
     * @param end   end date
     * @return if the period is invalid then return true, else return false.
     */
    boolean checkDate(String start, String end) {
        try {
            Date startDate = mDateFormat.parse(start);
            Date endDate = mDateFormat.parse(end);

            if (startDate.compareTo(endDate) > 0) {
                return true;
            } else if (startDate.compareTo(endDate) == 0) {
                return true;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Initialize Chart view -> Empty chart
     */
    void initializeChart() {
        mLineChart.setData(null);
        mLineChart.invalidate();

        Paint paint = mLineChart.getPaint(Chart.PAINT_INFO);
        paint.setTextSize(32f);
        mLineChart.setNoDataText("없졍");

        mStatResultText.setText(null);

        mStatStartEditText.setText(null);
        mStatEndEditText.setText(null);

        mCustomStart = null;
        mCustomEnd = null;
    }

    void updateChartGraph() {
        switch (selectedPeriod) {
            case WEEK_PERIOD:
                drawWeekPeriodStatisticsChart();
                break;
            case MONTH_PERIOD:
                drawMonthPeriodStatisticsChart();
                break;
            case CUSTOM_PERIOD:
                drawCustomPeriodStatisticsChart();
                break;
        }
    }

}

