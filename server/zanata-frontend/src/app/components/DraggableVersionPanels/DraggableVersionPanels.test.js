/* global jest describe it expect */

import React from 'react'
import * as ReactDOMServer from 'react-dom/server'
import DraggableVersionPanels, {Item, DragHandle, tooltipSort} from '.'
import {ListGroup, ListGroupItem, OverlayTrigger} from 'react-bootstrap'
import {LockIcon} from '../../components'
import Button from 'antd/lib/button'
import Icon from 'antd/lib/icon'

const callback = function (_e) {}

describe('DraggableVersionPanels', () => {
  it('can render a draggable Item', () => {
    const version = {
      projectSlug: 'meikai1',
      version: {
        id: 'ver1',
        status: 'ACTIVE'
      }
    }
    const actual = ReactDOMServer.renderToStaticMarkup(
      // @ts-ignore
      <Item key={'meikai1:ver1'} index={0}
        value={version} removeVersion={callback} />
    )
    const expected = ReactDOMServer.renderToStaticMarkup(
      <ListGroupItem className='v' >
        <DragHandle />
        {'ver1'} <span className='u-textMuted'> {'meikai1'}
        </span> <LockIcon status={'ACTIVE'} />
        {" "}
        <Button className='close rm-version-btn btn-xs' aria-label='button'
          onClick={callback} icon='close' />
      </ListGroupItem>
    )
    expect(actual).toEqual(expected)
  })
  it('can render DraggableVersionPanels', () => {
    const someVersions = [{
      projectSlug: 'meikai1',
      version: {
        id: 'ver1',
        status: 'ACTIVE'
      }
    },
      {
        projectSlug: 'meikai2',
        version: {
          id: 'ver2',
          status: 'ACTIVE'
        }
      }]
    const actual = ReactDOMServer.renderToStaticMarkup(
      <DraggableVersionPanels
        // @ts-ignore
        selectedVersions={someVersions}
        onDraggableMoveEnd={callback}
        removeVersion={callback} />
    )
    const expected = ReactDOMServer.renderToStaticMarkup(
      <ListGroup>
        <div>
          <div className='ant-layout'>
            <span className="versionMergeTitle-adjusted VersionMergeTitle">
            Adjust priority of selected versions
            </span><br />
            <span className="u-textMuted versionMergeTitle-sub">(best first)</span>
            <OverlayTrigger placement='top' overlay={tooltipSort}>
              <Button className="btn-xs btn-link iconInfoVersionMerge">
                <Icon type="info-circle-o" className="s0" />
              </Button>
            </OverlayTrigger>
            <Item
              // @ts-ignore
              key={'meikai1:ver1'} index={0}
              value={someVersions[0]} removeVersion={callback} />
            <Item
              // @ts-ignore
              key={'meikai2:ver2'} index={1}
              value={someVersions[1]} removeVersion={callback} />
          </div>
        </div>
      </ListGroup>
    )
    expect(actual).toEqual(expected)
  })
  it('returns an descriptive span if there are no selectedVersions', () => {
    const actual = ReactDOMServer.renderToStaticMarkup(
      <DraggableVersionPanels
        selectedVersions={[]}
        onDraggableMoveEnd={callback}
        removeVersion={callback} />
    )
    const expected = ReactDOMServer.renderToStaticMarkup(
      <span className="no-v text-muted">
        Please select versions to sort<br />
        <Icon type="api" className="s8" />
      </span>
    )
    expect(actual).toEqual(expected)
  })
})