package com.etheller.warsmash.viewer5.handlers.w3x.simulation;

import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.scope.TriggerExecutionScope;
import com.etheller.interpreter.ast.scope.trigger.Trigger;
import com.etheller.interpreter.ast.scope.trigger.TriggerBooleanExpression;
import com.etheller.warsmash.parsers.jass.scope.CommonTriggerExecutionScope;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.JassGameEventsWar3;

/**
 * A global event that has no associated widget (unit/item/destructable).
 * Used for game-level events such as EVENT_GAME_VICTORY, EVENT_GAME_END_LEVEL,
 * EVENT_GAME_SAVE, and EVENT_GAME_LOADED.
 */
public class CGlobalGameEvent extends CGlobalEvent {
	private final CSimulation game;
	private final GlobalScope globalScope;
	private final Trigger trigger;
	private final JassGameEventsWar3 eventType;
	private final TriggerBooleanExpression filter;

	public CGlobalGameEvent(final CSimulation game, final GlobalScope globalScope, final Trigger trigger,
			final JassGameEventsWar3 eventType, final TriggerBooleanExpression filter) {
		super(trigger);
		this.game = game;
		this.globalScope = globalScope;
		this.trigger = trigger;
		this.eventType = eventType;
		this.filter = filter;
	}

	@Override
	public Trigger getTrigger() {
		return this.trigger;
	}

	@Override
	public JassGameEventsWar3 getEventType() {
		return this.eventType;
	}

	@Override
	public void remove() {
		this.game.removeGlobalEvent(this);
	}

	@Override
	public void fire(final CWidget triggerWidget, final TriggerExecutionScope scope) {
		final CommonTriggerExecutionScope execScope = CommonTriggerExecutionScope.gameEventScope(this.eventType,
				this.trigger);
		this.globalScope.queueTrigger(this.filter, execScope, this.trigger, execScope, execScope);
	}
}
