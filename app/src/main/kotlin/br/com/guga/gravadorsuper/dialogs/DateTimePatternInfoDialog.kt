package br.com.guga.gravadorsuper.dialogs

import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.extensions.viewBinding
import br.com.guga.gravadorsuper.databinding.DatetimePatternInfoLayoutBinding
import br.com.guga.gravadorsuper.R

class DateTimePatternInfoDialog(activity: BaseSimpleActivity) {
    val binding by activity.viewBinding(DatetimePatternInfoLayoutBinding::inflate)

    init {
        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok) { _, _ -> { } }
            .apply {
                activity.setupDialogStuff(binding.root, this)
            }
    }
}
