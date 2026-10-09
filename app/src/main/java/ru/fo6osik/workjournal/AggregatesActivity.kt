package ru.fo6osik.workjournal

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class AggregatesActivity : AppCompatActivity() {

    private lateinit var aggregatesContainer: LinearLayout
    private lateinit var textEmptyAggregates: TextView

    private val database by lazy {
        DatabaseProvider.getDatabase(
            applicationContext
        )
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_aggregates
        )

        aggregatesContainer =
            findViewById(
                R.id.aggregatesContainer
            )

        textEmptyAggregates =
            findViewById(
                R.id.textEmptyAggregates
            )
    }

    override fun onResume() {
        super.onResume()

        loadAggregates()
    }

    private fun loadAggregates() {

        lifecycleScope.launch {

            val aggregates =
                database
                    .aggregateDao()
                    .getRootAggregates()

            aggregatesContainer.removeAllViews()

            textEmptyAggregates.visibility =
                if (
                    aggregates.isEmpty()
                ) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            for (
                aggregate in aggregates
            ) {

                addAggregateCard(
                    aggregate
                )
            }
        }
    }

    private suspend fun addAggregateCard(
        aggregate: Aggregate
    ) {

        val density =
            resources.displayMetrics.density

        val childrenCount =
            database
                .aggregateDao()
                .getChildren(
                    aggregate.id
                )
                .size

        val card =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                isClickable =
                    true

                isFocusable =
                    true

                setPadding(
                    (16 * density).toInt(),
                    (14 * density).toInt(),
                    (16 * density).toInt(),
                    (14 * density).toInt()
                )

                background =
                    GradientDrawable().apply {

                        cornerRadius =
                            18 * density

                        setColor(
                            Color.rgb(
                                35,
                                35,
                                35
                            )
                        )

                        setStroke(
                            (1 * density).toInt(),
                            Color.rgb(
                                80,
                                80,
                                80
                            )
                        )
                    }

                setOnClickListener {

                    startActivity(
                        Intent(
                            this@AggregatesActivity,
                            AggregateDetailsActivity::class.java
                        ).apply {

                            putExtra(
                                AggregateDetailsActivity.EXTRA_AGGREGATE_ID,
                                aggregate.id
                            )
                        }
                    )
                }
            }

        card.addView(
            TextView(this).apply {

                text =
                    aggregate.name

                textSize =
                    21f

                setTypeface(
                    typeface,
                    Typeface.BOLD
                )

                setTextColor(
                    Color.WHITE
                )
            }
        )

        card.addView(
            createInfoText(
                "Тип: ${aggregate.category}"
            )
        )

        aggregate.workingWidthM
            ?.let {

                card.addView(
                    createInfoText(
                        "Рабочая ширина: ${
                            formatNumber(it)
                        } м"
                    )
                )
            }

        if (
            childrenCount > 0
        ) {

            card.addView(
                createInfoText(
                    "Дочерних агрегатов: $childrenCount"
                )
            )
        }

        aggregate.note
            .takeIf {
                it.isNotBlank()
            }
            ?.let {

                card.addView(
                    createInfoText(
                        "Примечание: $it"
                    )
                )
            }

        aggregatesContainer.addView(
            card,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {

                bottomMargin =
                    (12 * density).toInt()
            }
        )
    }

    private fun createInfoText(
        value: String
    ): TextView {

        val density =
            resources.displayMetrics.density

        return TextView(this).apply {

            text =
                value

            textSize =
                15f

            setTextColor(
                Color.LTGRAY
            )

            setPadding(
                0,
                (5 * density).toInt(),
                0,
                0
            )
        }
    }

    private fun formatNumber(
        value: Double
    ): String {

        return if (
            value % 1.0 == 0.0
        ) {
            value.toInt().toString()
        } else {
            value
                .toString()
                .replace(
                    ".",
                    ","
                )
        }
    }
}
