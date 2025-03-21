/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import { Component } from 'react';
import { IconButton, InputLabel, Select, Typography, FormControl, MenuItem } from '@material-ui/core';
import './ServiceVersionDiff.css';

export default class ServiceVersionDiff extends Component {
    constructor(props) {
        const { version1, version2 } = props;
        super(props);
        this.state = {
            selectedVersion1: version1 ? { text: version1 } : undefined,
            selectedVersion2: version2 ? { text: version2 } : undefined,
        };

        this.handleVersion1Change = this.handleVersion1Change.bind(this);
        this.handleVersion2Change = this.handleVersion2Change.bind(this);
    }

    handleVersion1Change = (event) => {
        this.setState({ selectedVersion1: event.target.value });
    };

    handleVersion2Change = (event) => {
        this.setState({ selectedVersion2: event.target.value });
    };

    render() {
        const { serviceId, versions, getDiff, diffText } = this.props;
        const { selectedVersion1, selectedVersion2 } = this.state;
        const selectorStyle = {
            width: '140px',
        };
        return (
            <div className="api-diff-container">
                <div className="api-diff-form">
                    <Typography data-testid="compare-label">Compare</Typography>
                    <FormControl className="formField">
                        <InputLabel shrink>Version</InputLabel>
                        <Select
                            data-testid="select-1"
                            label="versionSelect1"
                            value={selectedVersion1}
                            onChange={this.handleVersion1Change}
                            sx={selectorStyle}
                        >
                            {versions.map((version) => (
                                <MenuItem data-testid="menu-items-1" value={version}>
                                    {version}
                                </MenuItem>
                            ))}
                        </Select>
                    </FormControl>
                    <Typography data-testid="label-with">with</Typography>
                    <FormControl className="formField">
                        <InputLabel shrink>Version</InputLabel>
                        <Select
                            data-testid="select-2"
                            label="versionSelect2"
                            value={selectedVersion2}
                            onChange={this.handleVersion2Change}
                            sx={selectorStyle}
                        >
                            {versions.map((version) => (
                                <MenuItem data-testid="menu-items-2" value={version}>
                                    {version}
                                </MenuItem>
                            ))}
                        </Select>
                    </FormControl>
                    <IconButton
                        disabled={!selectedVersion1 || !selectedVersion2}
                        id="diff-button"
                        data-testid="diff-button"
                        onClick={() => {
                            getDiff(serviceId, selectedVersion1, selectedVersion2);
                        }}
                    >
                        Go
                    </IconButton>
                </div>
                {/* eslint-disable-next-line react/no-danger */}
                <div className="api-diff-content" dangerouslySetInnerHTML={{ __html: diffText }} />
            </div>
        );
    }
}
