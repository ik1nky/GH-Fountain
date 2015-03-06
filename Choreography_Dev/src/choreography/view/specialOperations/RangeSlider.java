package choreography.view.specialOperations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.StyleOrigin;
import javafx.css.Styleable;
import javafx.css.StyleableBooleanProperty;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableIntegerProperty;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.geometry.Orientation;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.control.Slider;

import com.sun.javafx.Utils;
import com.sun.javafx.css.converters.BooleanConverter;
import com.sun.javafx.css.converters.EnumConverter;
import com.sun.javafx.css.converters.SizeConverter;

/**
 * The RangeSlider control is simply a JavaFX {@link Slider} control with support
 * for two 'thumbs', rather than one. A thumb is the non-technical name for the
 * draggable area inside the Slider / RangeSlider that allows for a value to be
 * set. 
 * 
 * Because the RangeSlider has two thumbs, it also has a few additional rules
 * and user interactions:
 * 
 *   The 'lower value' thumb can not move past the 'higher value' thumb.
 *   Whereas the {@link Slider} control only has one 
 *       {@link Slider#valueProperty() value} property, the RangeSlider has a 
 *       {@link #lowValueProperty() low value} and a 
 *       {@link #highValueProperty() high value} property, not surprisingly 
 *       represented by the 'low value' and 'high value' thumbs.
 *   The area between the low and high values represents the allowable range.
 *       For example, if the low value is 2 and the high value is 8, then the
 *       allowable range is between 2 and 8. 
 *   The allowable range area is rendered differently. This area is able to 
 *       be dragged with mouse / touch input to allow for the entire range to
 *       be modified. For example, following on from the previous example of the
 *       allowable range being between 2 and 8, if the user drags the range bar
 *       to the right, the low value will adjust to 3, and the high value 9, and
 *       so on until the user stops adjusting. 
 */
public class RangeSlider extends Control {
    
    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/
    
    /**
     * Creates a new RangeSlider instance using default values of 0.0, 0.25, 0.75
     * and 1.0 for min/lowValue/highValue/max, respectively. 
     */
    public RangeSlider() {
        this(0, 1.0, 0.25, 0.75);
    }

    /**
     * Instantiates a default, horizontal RangeSlider with the specified 
     * min/max/low/high values.
     * 
     * @param min The minimum allowable value that the RangeSlider will allow.
     * @param max The maximum allowable value that the RangeSlider will allow.
     * @param lowValue The initial value for the low value in the RangeSlider.
     * @param highValue The initial value for the high value in the RangeSlider.
     */
    public RangeSlider(double min, double max, double lowValue, double highValue) {
        getStyleClass().setAll(DEFAULT_STYLE_CLASS);
        
        setMax(max);
        setMin(min);
        adjustValues();
        setLowValue(lowValue);
        setHighValue(highValue);
    }
    
    /**
     * {@inheritDoc}
     * @return 
     */
    @Override
    public String getUserAgentStylesheet() {
        return getClass().getResource("rangeslider.css").toExternalForm();
    }
    
    /**
     * {@inheritDoc}
     * @return 
     */
    @Override protected Skin<?> createDefaultSkin() {
        return new RangeSliderSkin(this);
    }
  
    
    
    /***************************************************************************
     *                                                                         *
     * New properties (over and above what is in Slider)                       *
     *                                                                         *
     **************************************************************************/
    
    // --- low value
    /**
     * The low value property represents the current position of the low value
     * thumb, and is within the allowable range as specified by the
     * {@link #minProperty() min} and {@link #maxProperty() max} properties. By
     * default this value is 0.
     * @return 
     */
    public final DoubleProperty lowValueProperty() {
        return lowValue;
    }
    private DoubleProperty lowValue = new SimpleDoubleProperty(this, "lowValue", 0.0D) {
        @Override protected void invalidated() {
            adjustLowValues();
        }
    };
    
    /**
     * Sets the low value for the range slider, which may or may not be clamped
     * to be within the allowable range as specified by the
     * {@link #minProperty() min} and {@link #maxProperty() max} properties.
     * @param d
     */
    public final void setLowValue(double d) {
        lowValueProperty().set(d);
    }

    /**
     * Returns the current low value for the range slider.
     */
    public final double getLowValue() {
        return lowValue != null ? lowValue.get() : 0.0D;
    }

    
    
    // --- low value changing
    /**
     * When true, indicates the current low value of this RangeSlider is changing.
     * It provides notification that the low value is changing. Once the low 
     * value is computed, it is set back to false.
     */
    public final BooleanProperty lowValueChangingProperty() {
        if (lowValueChanging == null) {
            lowValueChanging = new SimpleBooleanProperty(this, "lowValueChanging", false);
        }
        return lowValueChanging;
    }
    
    private BooleanProperty lowValueChanging;

    /**
     * Call this when the low value is changing.
     * @param value True if the low value is changing, false otherwise.
     */
    public final void setLowValueChanging(boolean value) {
        lowValueChangingProperty().set(value);
    }

    /**
     * Returns whether or not the low value of this RangeSlider is currently
     * changing.
     */
    public final boolean isLowValueChanging() {
        return lowValueChanging == null ? false : lowValueChanging.get();
    }

    
    // --- high value
    /**
     * The high value property represents the current position of the high value
     * thumb, and is within the allowable range as specified by the
     * {@link #minProperty() min} and {@link #maxProperty() max} properties. By
     * default this value is 100.
     */
    public final DoubleProperty highValueProperty() {
        return highValue;
    }
    private DoubleProperty highValue = new SimpleDoubleProperty(this, "highValue", 100D) {
        @Override protected void invalidated() {
            adjustHighValues();
        }
        
        @Override public Object getBean() {
            return RangeSlider.this;
        }

        @Override public String getName() {
            return "highValue";
        }
    };
    
    /**
     * Sets the high value for the range slider, which may or may not be clamped
     * to be within the allowable range as specified by the
     * {@link #minProperty() min} and {@link #maxProperty() max} properties.
     */
    public final void setHighValue(double d) {
        if (!highValueProperty().isBound()) highValueProperty().set(d);
    }

    /**
     * Returns the current high value for the range slider.
     */
    public final double getHighValue() {
        return highValue != null ? highValue.get() : 100D;
    }

    

    // --- high value changing
    /**
     * When true, indicates the current high value of this RangeSlider is changing.
     * It provides notification that the high value is changing. Once the high 
     * value is computed, it is set back to false.
     */
    public final BooleanProperty highValueChangingProperty() {
        if (highValueChanging == null) {
            highValueChanging = new SimpleBooleanProperty(this, "highValueChanging", false);
        }
        return highValueChanging;
    }
    private BooleanProperty highValueChanging;

    /**
     * Call this when high low value is changing.
     * @param value True if the high value is changing, false otherwise.
     */
    public final void setHighValueChanging(boolean value) {
        highValueChangingProperty().set(value);
    }

    /**
     * Returns whether or not the high value of this RangeSlider is currently
     * changing.
     */
    public final boolean isHighValueChanging() {
        return highValueChanging == null ? false : highValueChanging.get();
    }
    
    
    
    
    
    /***************************************************************************
     *                                                                         *
     * New public API                                                          *
     *                                                                         *
     **************************************************************************/
    
    /**
     * Increments the {@link #lowValueProperty() low value} by the 
     * {@link #blockIncrementProperty() block increment} amount.
     */
    public void incrementLowValue() {
        adjustLowValue(getLowValue() + getBlockIncrement());
    }

    /**
     * Decrements the {@link #lowValueProperty() low value} by the 
     * {@link #blockIncrementProperty() block increment} amount.
     */
    public void decrementLowValue() {
        adjustLowValue(getLowValue() - getBlockIncrement());
    }
    
    /**
     * Increments the {@link #highValueProperty() high value} by the 
     * {@link #blockIncrementProperty() block increment} amount.
     */
    public void incrementHighValue() {
        adjustHighValue(getHighValue() + getBlockIncrement());
    }

    /**
     * Decrements the {@link #highValueProperty() high value} by the 
     * {@link #blockIncrementProperty() block increment} amount.
     */
    public void decrementHighValue() {
        adjustHighValue(getHighValue() - getBlockIncrement());
    }
    
    /**
     * Adjusts {@link #lowValueProperty() lowValue} to match <code>newValue</code>,
     * or as closely as possible within the constraints imposed by the 
     * {@link #minProperty() min} and {@link #maxProperty() max} properties. 
     * This function also takes into account 
     * {@link #snapToTicksProperty() snapToTicks}, which is the main difference 
     * between <code>adjustLowValue</code> and 
     * {@link #setLowValue(double) setLowValue}.
     */
    public void adjustLowValue(double newValue) {
        double d1 = getMin();
        double d2 = getMax();
        if (d2 <= d1) {
            // no-op
        } else {
            newValue = newValue >= d1 ? newValue : d1;
            newValue = newValue <= d2 ? newValue : d2;
            setLowValue(snapValueToTicks(newValue));
        }
    }

    /**
     * Adjusts {@link #highValueProperty() highValue} to match <code>newValue</code>,
     * or as closely as possible within the constraints imposed by the 
     * {@link #minProperty() min} and {@link #maxProperty() max} properties. 
     * This function also takes into account 
     * {@link #snapToTicksProperty() snapToTicks}, which is the main difference 
     * between <code>adjustHighValue</code> and 
     * {@link #setHighValue(double) setHighValue}.
     */
    public void adjustHighValue(double newValue) {
        double d1 = getMin();
        double d2 = getMax();
        if (d2 <= d1) {
            // no-op
        } else {
            newValue = newValue >= d1 ? newValue : d1;
            newValue = newValue <= d2 ? newValue : d2;
            setHighValue(snapValueToTicks(newValue));
        }
    }

    
    
    /***************************************************************************
     *                                                                         *
     * Properties copied from Slider (and slightly edited)                     *
     *                                                                         *
     **************************************************************************/
    
    
    /**
     * The maximum value represented by this Slider. This must be a
     * value greater than {@link #minProperty() min}.
     */
    private DoubleProperty max;
    public final void setMax(double value) {
        maxProperty().set(value);
    }

    public final double getMax() {
        return max == null ? 100 : max.get();
    }

    public final DoubleProperty maxProperty() {
        if (max == null) {
            max = new DoublePropertyBase(100) {
                @Override protected void invalidated() {
                    if (get() < getMin()) {
                        setMin(get());
                    }
                    adjustValues();
                }

                @Override public Object getBean() {
                    return RangeSlider.this;
                }

                @Override public String getName() {
                    return "max";
                }
            };
        }
        return max;
    }
    /**
     * The minimum value represented by this Slider. This must be a
     * value less than {@link #maxProperty() max}.
     */
    private DoubleProperty min;
    public final void setMin(double value) {
        minProperty().set(value);
    }

    public final double getMin() {
        return min == null ? 0 : min.get();
    }

    public final DoubleProperty minProperty() {
        if (min == null) {
            min = new DoublePropertyBase(0) {
                @Override protected void invalidated() {
                    if (get() > getMax()) {
                        setMax(get());
                    }
                    adjustValues();
                }

                @Override public Object getBean() {
                    return RangeSlider.this;
                }

                @Override public String getName() {
                    return "min";
                }
            };
        }
        return min;
    }
    
    /**
     * Indicates whether the {@link #lowValueProperty()} value}/{@link #highValueProperty()} value} of the {@code Slider} should always
     * be aligned with the tick marks. This is honored even if the tick marks
     * are not shown.
     */
    private BooleanProperty snapToTicks;
    public final void setSnapToTicks(boolean value) {
        snapToTicksProperty().set(value);
    }

    public final boolean isSnapToTicks() {
        return snapToTicks == null ? false : snapToTicks.get();
    }

    public final BooleanProperty snapToTicksProperty() {
        if (snapToTicks == null) {
            snapToTicks = new StyleableBooleanProperty(false) {
                @Override public CssMetaData<? extends Styleable, Boolean> getCssMetaData() {
                    return RangeSlider.StyleableProperties.SNAP_TO_TICKS;
                }

                @Override public Object getBean() {
                    return RangeSlider.this;
                }

                @Override public String getName() {
                    return "snapToTicks";
                }
            };
        }
        return snapToTicks;
    }
    /**
     * The unit distance between major tick marks. For example, if
     * the {@link #minProperty() min} is 0 and the {@link #maxProperty() max} is 100 and the
     * {@link #majorTickUnitProperty() majorTickUnit} is 25, then there would be 5 tick marks: one at
     * position 0, one at position 25, one at position 50, one at position
     * 75, and a final one at position 100.
     * <p>
     * This value should be positive and should be a value less than the
     * span. Out of range values are essentially the same as disabling
     * tick marks.
     */
    private DoubleProperty majorTickUnit;
    public final void setMajorTickUnit(double value) {
        if (value <= 0) {
            throw new IllegalArgumentException("MajorTickUnit cannot be less than or equal to 0.");
        }
        majorTickUnitProperty().set(value);
    }

    public final double getMajorTickUnit() {
        return majorTickUnit == null ? 25 : majorTickUnit.get();
    }

    public final DoubleProperty majorTickUnitProperty() {
        if (majorTickUnit == null) {
            majorTickUnit = new StyleableDoubleProperty(25) {
                @Override public void invalidated() {
                    if (get() <= 0) {
                        throw new IllegalArgumentException("MajorTickUnit cannot be less than or equal to 0.");
                    }
                }
                
                @Override public CssMetaData<? extends Styleable, Number> getCssMetaData() {
                    return StyleableProperties.MAJOR_TICK_UNIT;
                }

                @Override public Object getBean() {
                    return RangeSlider.this;
                }

                @Override public String getName() {
                    return "majorTickUnit";
                }
            };
        }
        return majorTickUnit;
    }
    /**
     * The number of minor ticks to place between any two major ticks. This
     * number should be positive or zero. Out of range values will disable
     * minor ticks, as will a value of zero.
     */
    private IntegerProperty minorTickCount;
    public final void setMinorTickCount(int value) {
        minorTickCountProperty().set(value);
    }

    public final int getMinorTickCount() {
        return minorTickCount == null ? 3 : minorTickCount.get();
    }

    public final IntegerProperty minorTickCountProperty() {
        if (minorTickCount == null) {
            minorTickCount = new StyleableIntegerProperty(3) {
                @Override public CssMetaData<? extends Styleable, Number> getCssMetaData() {
                    return RangeSlider.StyleableProperties.MINOR_TICK_COUNT;
                }

                @Override public Object getBean() {
                    return RangeSlider.this;
                }

                @Override public String getName() {
                    return "minorTickCount";
                }
            };
        }
        return minorTickCount;
    }
    /**
     * The amount by which to adjust the slider if the track of the slider is
     * clicked. This is used when manipulating the slider position using keys. If
     * {@link #snapToTicksProperty() snapToTicks} is true then the nearest tick mark to the adjusted
     * value will be used.
     */
    private DoubleProperty blockIncrement;
    public final void setBlockIncrement(double value) {
        blockIncrementProperty().set(value);
    }

    public final double getBlockIncrement() {
        return blockIncrement == null ? 10 : blockIncrement.get();
    }

    public final DoubleProperty blockIncrementProperty() {
        if (blockIncrement == null) {
            blockIncrement = new StyleableDoubleProperty(10) {
                @Override public CssMetaData<? extends Styleable, Number> getCssMetaData() {
                    return RangeSlider.StyleableProperties.BLOCK_INCREMENT;
                }

                @Override public Object getBean() {
                    return RangeSlider.this;
                }

                @Override public String getName() {
                    return "blockIncrement";
                }
            };
        }
        return blockIncrement;
    }
    
    /**
     * The orientation of the {@code Slider} can either be horizontal
     * or vertical.
     */
    private ObjectProperty<Orientation> orientation;
    public final void setOrientation(Orientation value) {
        orientationProperty().set(value);
    }

    public final Orientation getOrientation() {
        return orientation == null ? Orientation.HORIZONTAL : orientation.get();
    }

    public final ObjectProperty<Orientation> orientationProperty() {
        if (orientation == null) {
            orientation = new StyleableObjectProperty<Orientation>(Orientation.HORIZONTAL) {
                @Override protected void invalidated() {
                    final boolean vertical = (get() == Orientation.VERTICAL);
                    pseudoClassStateChanged(VERTICAL_PSEUDOCLASS_STATE, vertical);
                    pseudoClassStateChanged(HORIZONTAL_PSEUDOCLASS_STATE, ! vertical);
                }
                
                @Override public CssMetaData<? extends Styleable, Orientation> getCssMetaData() {
                    return RangeSlider.StyleableProperties.ORIENTATION;
                }

                @Override public Object getBean() {
                    return RangeSlider.this;
                }

                @Override public String getName() {
                    return "orientation";
                }
            };
        }
        return orientation;
    }
    
    /**
     * Indicates that the labels for tick marks should be shown. Typically a
     * {@link Skin} implementation will only show labels if
     * {@link #showTickMarksProperty() showTickMarks} is also true.
     */
    private BooleanProperty showTickLabels;
    public final void setShowTickLabels(boolean value) {
        showTickLabelsProperty().set(value);
    }

    public final boolean isShowTickLabels() {
        return showTickLabels == null ? false : showTickLabels.get();
    }

    public final BooleanProperty showTickLabelsProperty() {
        if (showTickLabels == null) {
            showTickLabels = new StyleableBooleanProperty(false) {
                @Override public CssMetaData<? extends Styleable, Boolean> getCssMetaData() {
                    return RangeSlider.StyleableProperties.SHOW_TICK_LABELS;
                }

                @Override public Object getBean() {
                    return RangeSlider.this;
                }

                @Override public String getName() {
                    return "showTickLabels";
                }
            };
        }
        return showTickLabels;
    }
    /**
     * Specifies whether the {@link Skin} implementation should show tick marks.
     */
    private BooleanProperty showTickMarks;
    public final void setShowTickMarks(boolean value) {
        showTickMarksProperty().set(value);
    }

    public final boolean isShowTickMarks() {
        return showTickMarks == null ? false : showTickMarks.get();
    }

    public final BooleanProperty showTickMarksProperty() {
        if (showTickMarks == null) {
            showTickMarks = new StyleableBooleanProperty(false) {
                @Override public CssMetaData<? extends Styleable, Boolean> getCssMetaData() {
                    return RangeSlider.StyleableProperties.SHOW_TICK_MARKS;
                }

                @Override public Object getBean() {
                    return RangeSlider.this;
                }

                @Override public String getName() {
                    return "showTickMarks";
                }
            };
        }
        return showTickMarks;
    }
    

    
     /***************************************************************************
     *                                                                         *
     * Private methods                                                         *
     *                                                                         *
     **************************************************************************/    
    
    /**
     * Ensures that min is always < max, that value is always
     * somewhere between the two, and that if snapToTicks is set then the
     * value will always be set to align with a tick mark.
     */
    private void adjustValues() {
        adjustLowValues();
        adjustHighValues();
    }

    private void adjustLowValues() {
        if (getLowValue() < getMin() || getLowValue() >= getHighValue()) {
            double value = Utils.clamp(getMin(), getLowValue(), getHighValue());
            setLowValue(value);
        }
    }
    
    private double snapValueToTicks(double d) {
        double d1 = d;
        if (isSnapToTicks()) {
            double d2 = 0.0D;
            if (getMinorTickCount() != 0) {
                d2 = getMajorTickUnit() / (double) (Math.max(getMinorTickCount(), 0) + 1);
            } else {
                d2 = getMajorTickUnit();
            }
            int i = (int) ((d1 - getMin()) / d2);
            double d3 = (double) i * d2 + getMin();
            double d4 = (double) (i + 1) * d2 + getMin();
            d1 = Utils.nearest(d3, d1, d4);
        }
        return Utils.clamp(getMin(), d1, getMax());
    }

    private void adjustHighValues() {
        if (getHighValue() < getLowValue() || getHighValue() > getMax()) {
            setHighValue(Utils.clamp(getLowValue(), getHighValue(), getMax()));
        }
    }

    
    
    /**************************************************************************
    *                                                                         *
    * Stylesheet Handling                                                     *
    *                                                                         *
    **************************************************************************/
    
    private static final String DEFAULT_STYLE_CLASS = "range-slider";
    
    private static class StyleableProperties {
        private static final CssMetaData<RangeSlider,Number> BLOCK_INCREMENT =
            new CssMetaData<RangeSlider,Number>("-fx-block-increment",
                SizeConverter.getInstance(), 10.0) {

            @Override public boolean isSettable(RangeSlider n) {
                return n.blockIncrement == null || !n.blockIncrement.isBound();
            }

            @SuppressWarnings("unchecked")
            @Override public StyleableProperty<Number> getStyleableProperty(RangeSlider n) {
                return (StyleableProperty<Number>)n.blockIncrementProperty();
            }
        };
        
        private static final CssMetaData<RangeSlider,Boolean> SHOW_TICK_LABELS =
            new CssMetaData<RangeSlider,Boolean>("-fx-show-tick-labels",
                BooleanConverter.getInstance(), Boolean.FALSE) {

            @Override public boolean isSettable(RangeSlider n) {
                return n.showTickLabels == null || !n.showTickLabels.isBound();
            }

            @SuppressWarnings("unchecked")
            @Override public StyleableProperty<Boolean> getStyleableProperty(RangeSlider n) {
                return (StyleableProperty<Boolean>)n.showTickLabelsProperty();
            }
        };
                    
        private static final CssMetaData<RangeSlider,Boolean> SHOW_TICK_MARKS =
            new CssMetaData<RangeSlider,Boolean>("-fx-show-tick-marks",
                BooleanConverter.getInstance(), Boolean.FALSE) {

            @Override public boolean isSettable(RangeSlider n) {
                return n.showTickMarks == null || !n.showTickMarks.isBound();
            }

            @SuppressWarnings("unchecked")
            @Override public StyleableProperty<Boolean> getStyleableProperty(RangeSlider n) {
                return (StyleableProperty<Boolean>)n.showTickMarksProperty();
            }
        };
            
        private static final CssMetaData<RangeSlider,Boolean> SNAP_TO_TICKS =
            new CssMetaData<RangeSlider,Boolean>("-fx-snap-to-ticks",
                BooleanConverter.getInstance(), Boolean.FALSE) {

            @Override public boolean isSettable(RangeSlider n) {
                return n.snapToTicks == null || !n.snapToTicks.isBound();
            }

            @SuppressWarnings("unchecked")
            @Override public StyleableProperty<Boolean> getStyleableProperty(RangeSlider n) {
                return (StyleableProperty<Boolean>)n.snapToTicksProperty();
            }
        };
        
        private static final CssMetaData<RangeSlider,Number> MAJOR_TICK_UNIT =
            new CssMetaData<RangeSlider,Number>("-fx-major-tick-unit",
                SizeConverter.getInstance(), 25.0) {

            @Override public boolean isSettable(RangeSlider n) {
                return n.majorTickUnit == null || !n.majorTickUnit.isBound();
            }

            @SuppressWarnings("unchecked")
            @Override public StyleableProperty<Number> getStyleableProperty(RangeSlider n) {
                return (StyleableProperty<Number>)n.majorTickUnitProperty();
            }
        };
        
        private static final CssMetaData<RangeSlider,Number> MINOR_TICK_COUNT =
            new CssMetaData<RangeSlider,Number>("-fx-minor-tick-count",
                SizeConverter.getInstance(), 3.0) {

            @SuppressWarnings("deprecation")
            @Override public void set(RangeSlider node, Number value, StyleOrigin origin) {
                super.set(node, value.intValue(), origin);
            } 
            
            @Override public boolean isSettable(RangeSlider n) {
                return n.minorTickCount == null || !n.minorTickCount.isBound();
            }

            @SuppressWarnings("unchecked")
            @Override public StyleableProperty<Number> getStyleableProperty(RangeSlider n) {
                return (StyleableProperty<Number>)n.minorTickCountProperty();
            }
        };
        
        private static final CssMetaData<RangeSlider,Orientation> ORIENTATION =
            new CssMetaData<RangeSlider,Orientation>("-fx-orientation",
                new EnumConverter<Orientation>(Orientation.class), 
                Orientation.HORIZONTAL) {

            @Override public Orientation getInitialValue(RangeSlider node) {
                // A vertical Slider should remain vertical 
                return node.getOrientation();
            }

            @Override public boolean isSettable(RangeSlider n) {
                return n.orientation == null || !n.orientation.isBound();
            }

            @SuppressWarnings("unchecked")
            @Override public StyleableProperty<Orientation> getStyleableProperty(RangeSlider n) {
                return (StyleableProperty<Orientation>)n.orientationProperty();
            }
        };

        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
        static {
            final List<CssMetaData<? extends Styleable, ?>> styleables = 
                    new ArrayList<CssMetaData<? extends Styleable, ?>>(Control.getClassCssMetaData());
            styleables.add(BLOCK_INCREMENT);
            styleables.add(SHOW_TICK_LABELS);
            styleables.add(SHOW_TICK_MARKS);
            styleables.add(SNAP_TO_TICKS);
            styleables.add(MAJOR_TICK_UNIT);
            styleables.add(MINOR_TICK_COUNT);
            styleables.add(ORIENTATION);

            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }
    

    /**
     * @return The CssMetaData associated with this class, which may include the
     * CssMetaData of its super classes.
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    /**
     * RT-19263
     * @return 
     * @deprecated This is an experimental API that is not intended for general use and is subject to change in future versions
     */
    @Deprecated
    @Override protected List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
        return getClassCssMetaData();
    }

    private static final PseudoClass VERTICAL_PSEUDOCLASS_STATE =
            PseudoClass.getPseudoClass("vertical");
    private static final PseudoClass HORIZONTAL_PSEUDOCLASS_STATE =
            PseudoClass.getPseudoClass("horizontal");
}