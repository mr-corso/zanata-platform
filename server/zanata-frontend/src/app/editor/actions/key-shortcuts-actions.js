// @ts-nocheck
import { getSaveButtonStatus, hasTranslationChanged }
  from '../utils/phrase-util'
import { createAction } from 'redux-actions'
import {
  copyFromAlignedSource,
  undoEdit,
  cancelEdit,
  savePhraseWithStatus,
  toggleSaveErrorModal } from './phrases-actions'
import { moveNext, movePrevious } from './phrase-navigation-actions'
import { copySuggestionN } from './suggestions-actions'
import { toggleReviewModal } from './review-trans-actions'
import {
  STATUS_TRANSLATED,
  STATUS_NEEDS_WORK,
  STATUS_REJECTED,
  STATUS_APPROVED
} from '../utils/status-util'

function shortcutInfo (keys, eventActionCreator, description, eventType) {
  keys = Array.isArray(keys) ? keys : [keys]
  return {
    keyConfig: {
      // Array of key combinations that each trigger this action
      keys,
      // (optional) keydown/keyup/keypress
      eventType
    },
    // When the key combination is pressed, this is passed the event and should
    // return a redux action.
    eventActionCreator,
    // Displayed on the key shortcut cheatsheet
    description
  }
}

/**
 * Shortcut definitions (key combinations, callback, description and sequences).
 *
 * See shortcutInfo(...) for the expected structure.
 *
 * CAUTION: with sequence keys, shortcuts are added and removed. They will
 *          clobber any other shortcut that uses the same keys. Never use the
 *          same key combination for a sequence that is used in any of the top-
 *          level shortcuts.
 *          Note: this could be fixed if we make use of Combokeys' sequences,
 *                but we lose the ability to trigger an action after the initial
 *                combination that starts the sequence.
 *
 * mod will be replaced by ctrl if on windows/linux or cmd if on mac.
 * By default it listens on keydown event.
 */
export const SHORTCUTS = {
  COPY_SOURCE: shortcutInfo(['alt+c', 'alt+g'],
      copyFromSourceActionCreator, 'Copy source as translation'),

  COPY_SUGGESTION_1: shortcutInfo(
    'mod+alt+1', copySuggestionActionCreator(1),
    'Copy first suggestion as translation'),

  COPY_SUGGESTION_2: shortcutInfo(
    'mod+alt+2', copySuggestionActionCreator(2),
    'Copy second suggestion as translation'),

  COPY_SUGGESTION_3: shortcutInfo(
    'mod+alt+3', copySuggestionActionCreator(3),
    'Copy third suggestion as translation'),

  COPY_SUGGESTION_4: shortcutInfo(
    'mod+alt+4', copySuggestionActionCreator(4),
    'Copy fourth suggestion as translation'),

  CANCEL_EDIT: shortcutInfo(
    'esc', cancelEditActionCreator,
    'Cancel edit'),

  SAVE_AS_CURRENT_BUTTON_OPTION: shortcutInfo(
    'mod+s', saveAsCurrentActionCreator, 'Save'),

  // TODO open the save dropdown and show prominently the letters to press for
  //      each option (bold in the text itself, or next to it, the latter may
  //      be better for l10n).
  //      Also show [Esc] to cancel.
  SAVE_AS_MODE: {
    keyConfig: {
      keys: ['mod+shift+s'],
      sequenceKeys: [
        shortcutInfo(['n', 'N'],
          saveAs(STATUS_NEEDS_WORK), 'Save as Needs Work'),
        shortcutInfo(['t', 'T'],
          saveAs(STATUS_TRANSLATED), 'Save as Translated'),
        shortcutInfo(['r', 'R'], saveAs(STATUS_REJECTED), 'Save as Rejected'),
        shortcutInfo(['a', 'A'], saveAs(STATUS_APPROVED), 'Save as Approved')
      ]
    },
    eventActionCreator: saveAsModeActionCreator,
    description: 'Save as...'
  },

  GOTO_NEXT_ROW_FAST: shortcutInfo(['mod+enter', 'alt+k', 'alt+down'],
    makeRowNavigationActionCreator(moveNext),
    'Save (if changed) and go to next string'),

  GOTO_PREVIOUS_ROW: shortcutInfo(['mod+shift+enter', 'alt+j', 'alt+up'],
    makeRowNavigationActionCreator(movePrevious),
    'Save (if changed) and go to previous string')
/*
 Disabled for now until status navigation implementation
 GOTO_NEXT_UNTRANSLATED: new ShortcutInfo(
 'tab+u', gotoToNextUntranslatedCallback)
 */
}

export function copyFromSourceActionCreator (event) {
  event.preventDefault()
  return copyFromAlignedSource()
}

/**
 * Generate a callback that will copy one of the suggestions to the editor.
 *
 * @param {number} oneBasedIndex the 1-based index of the suggestion that
 *                               this callback will copy
 * @return {function} callback that will copy the nth suggestion.
 */
function copySuggestionActionCreator (oneBasedIndex) {
  return (event) => {
    return (dispatch, getState) => {
      if (getState().phrases.selectedPhraseId) {
        const zeroBasedIndex = oneBasedIndex - 1
        event.preventDefault()
        dispatch(copySuggestionN(zeroBasedIndex))
      }
    }
  }
}

/**
 * The generic [Esc] key action, does different things depending on state.
 *
 * Does the first of these actions that has the right condition
 *  - if save-as mode is on, cancels save-as mode
 *  - if text has been changed, reverts to last save (FIXME this seems bad)
 *  - de-select the current phrase (if any)
 */
function cancelEditActionCreator (event) {
  event.preventDefault()
  event.stopPropagation()
  return (dispatch, getState) => {
    const { detail, selectedPhraseId } = getState().phrases
    if (selectedPhraseId) {
      const phrase = detail[selectedPhraseId]
      if (hasTranslationChanged(phrase)) {
        dispatch(undoEdit())
      } else {
        dispatch(cancelEdit())
      }
    }
  }
}

function saveAsCurrentActionCreator (event) {
  return (dispatch, getState) => {
    const { selectedPhraseId, detail } = getState().phrases
    if (selectedPhraseId) {
      event.preventDefault()
      const phrase = detail[selectedPhraseId]
      // skip if the button would be disabled
      // TODO move the save-allowed logic to a central location
      const isSaving = !!phrase.inProgressSave
      if (isSaving || !hasTranslationChanged(phrase)) {
        return
      }
      const status = getSaveButtonStatus(phrase)
      dispatch(savePhraseWithStatus(phrase, status))
    }
  }
}

/**
* Indicate that save-as mode is active or inactive.
*/
export const SET_SAVE_AS_MODE = 'SET_SAVE_AS_MODE'
export const setSaveAsMode = createAction(SET_SAVE_AS_MODE)

/**
 * This is to mimic sequence shortcut.
 * e.g. press ctrl-shift-s then press 'n' to save as 'needs work'.
 */
function saveAsModeActionCreator (event) {
  return (dispatch, getState) => {
    const { selectedPhraseId } = getState().phrases
    if (selectedPhraseId) {
      event.preventDefault()
      dispatch(setSaveAsMode(true))
    }
  }
}

function saveAs (status) {
  return (event) => {
    return (dispatch, getState) => {
      const { detail, selectedPhraseId } = getState().phrases
      if (selectedPhraseId) {
        // it would be more surprising to type a character when trying to save
        // as a disabled state, so always swallow the key event even if the save
        // operation is disabled.
        event.preventDefault()

        const phrase = detail[selectedPhraseId]
        // skip if the button would be disabled
        // TODO move the save-allowed logic to a central location
        const isSaving = !!phrase.inProgressSave
        const isCurrentStatus = phrase.status === status
        if (isSaving || (isCurrentStatus && !hasTranslationChanged(phrase))) {
          return
        }
        if (status === STATUS_REJECTED) {
          dispatch(toggleReviewModal())
        } else {
          dispatch(savePhraseWithStatus(phrase, status))
        }
      }
    }
  }
}

/**
 * Make an action creator for moveNext or movePrevious
 */
function makeRowNavigationActionCreator (operation) {
  return (event) => {
    return (dispatch, getState) => {
      const { detail, selectedPhraseId } = getState().phrases
      if (selectedPhraseId) {
        event.preventDefault()
        event.stopPropagation()
        const phrase = detail[selectedPhraseId]
        const translationChanged = hasTranslationChanged(phrase)
        // Catch saving with validation errors
        if (translationChanged && phrase.errors) {
          dispatch(toggleSaveErrorModal(phrase.id, true))
        } else {
          // Save if translation edited
          if (translationChanged) {
            dispatch(saveAsCurrentActionCreator(event))
          }
          // Move to next row
          dispatch(operation())
        }
      }
    }
  }
}

/*
 Disable for now until status navigation implementation

 function gotoToNextUntranslatedCallback(event) {
 event.preventDefault()
 event.stopPropagation()
 if (editorShortcuts.selectedTUCtrl) {
 console.log('GOTO_NEXT_UNTRANSLATED,
 currentContext())
 }
 // the shortcut is a tab + u combination
 // we don't want other tab event to trigger
 tabCombinationPressed = true
 }
 */
