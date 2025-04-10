document.addEventListener('DOMContentLoaded', function() {
    // Base URL configuration
    const isFileProtocol = window.location.protocol === 'file:';
    
    // Always use http://localhost:8080 for API requests regardless of how the frontend is accessed
    // This ensures we connect to the ZIO HTTP server for API calls, even when the frontend
    // is served from a different port (like the Python HTTP server on port 8083)
    let apiBaseUrl = 'http://localhost:8080';
    
    // Allow overriding through localStorage for debugging/development
    if (localStorage.getItem('workflowApiBaseUrl')) {
        apiBaseUrl = localStorage.getItem('workflowApiBaseUrl');
    
    }
    
    console.log(`Using API base URL: ${apiBaseUrl} (file protocol: ${isFileProtocol})`);
    
    // Helper function to get the full API URL
    function getApiUrl(path) {
        // Make sure path starts with /
        const normalizedPath = path.startsWith('/') ? path : `/${path}`;
        return `${apiBaseUrl}${normalizedPath}`;
    }
    
    // State
    let workflow = {
        nodes: [],
        connections: [],
        nextNodeId: 1,
        nextConnectionId: 1,
        selectedNode: null,
        currentExecutionId: null
    };

    // DOM elements
    const canvas = document.getElementById('workflow-canvas');
    const nodeProperties = document.getElementById('node-properties');
    const propertyForm = document.querySelector('.property-form');
    const nodeLabel = document.getElementById('node-label');
    const nodeConfig = document.getElementById('node-config');
    const applyPropertiesButton = document.getElementById('apply-properties');
    const executeButton = document.getElementById('execute-workflow');
    const cancelButton = document.getElementById('cancel-workflow');
    const saveButton = document.getElementById('save-workflow');
    const loadExampleButton = document.getElementById('load-example');
    const workflowInput = document.getElementById('workflow-input');
    const executionStatus = document.getElementById('execution-status');
    const executionResults = document.getElementById('execution-results');
    const progressContainer = document.getElementById('progress-container');
    const progressFill = document.getElementById('progress-fill');
    const progressPercentage = document.getElementById('progress-percentage');
    const loadingIndicator = document.getElementById('loading-indicator');
    const historyList = document.getElementById('history-list');
    const workflowHistory = document.querySelector('.workflow-history');

    // Initialize drag-and-drop for agent items
    const agentItems = document.querySelectorAll('.agent-item');
    agentItems.forEach(item => {
        item.addEventListener('dragstart', handleDragStart);
    });

    // Canvas event listeners
    canvas.addEventListener('dragover', handleDragOver);
    canvas.addEventListener('drop', handleDrop);
    canvas.addEventListener('click', handleCanvasClick);

    // Button event listeners
    applyPropertiesButton.addEventListener('click', applyNodeProperties);
    executeButton.addEventListener('click', executeWorkflow);
    cancelButton.addEventListener('click', cancelWorkflow);
    saveButton.addEventListener('click', saveWorkflow);
    loadExampleButton.addEventListener('click', loadExampleWorkflow);

    /**
     * Handle drag start event
     */
    function handleDragStart(e) {
        e.dataTransfer.setData('nodeType', this.getAttribute('data-type'));
    }

    /**
     * Handle drag over on canvas
     */
    function handleDragOver(e) {
        e.preventDefault();
    }

    /**
     * Handle dropping an agent item onto the canvas
     */
    function handleDrop(e) {
        e.preventDefault();
        const nodeType = e.dataTransfer.getData('nodeType');
        if (!nodeType) return;

        // Get canvas bounds and offset
        const canvasRect = canvas.getBoundingClientRect();
        const x = e.clientX - canvasRect.left;
        const y = e.clientY - canvasRect.top;

        createNode(nodeType, x, y);
    }

    /**
     * Create a new node on the canvas
     */
    function createNode(nodeType, x, y) {
        const nodeId = `node-${workflow.nextNodeId++}`;
        const node = {
            id: nodeId,
            type: nodeType,
            label: getLabelFromType(nodeType),
            config: {},
            position: { x, y }
        };

        workflow.nodes.push(node);
        renderNode(node);
    }

    /**
     * Get default label from node type
     */
    function getLabelFromType(type) {
        switch (type) {
            case 'text-transformer': return 'Capitalize Text';
            case 'text-splitter': return 'Split Text';
            case 'summarizer': return 'Summarize Text';
            case 'build': return 'Build Agent';
            default: return 'Node';
        }
    }

    /**
     * Render a node on the canvas
     */
    function renderNode(node) {
        const nodeElement = document.createElement('div');
        nodeElement.className = `node node-${node.type}`;
        nodeElement.id = node.id;
        nodeElement.dataset.type = node.type;
        nodeElement.style.left = `${node.position.x - 60}px`;
        nodeElement.style.top = `${node.position.y - 30}px`;

        nodeElement.innerHTML = `
            <div class="node-title">${node.label}</div>
            <div class="node-type">${node.type}</div>
            <div class="input-connector connector" data-node="${node.id}" data-connector="input"></div>
            <div class="output-connector connector" data-node="${node.id}" data-connector="output"></div>
        `;

        canvas.appendChild(nodeElement);

        // Make node draggable
        nodeElement.addEventListener('mousedown', startDragNode);
        
        // Make connectors handle connections
        const inputConnector = nodeElement.querySelector('.input-connector');
        const outputConnector = nodeElement.querySelector('.output-connector');
        
        inputConnector.addEventListener('mousedown', startConnection);
        outputConnector.addEventListener('mousedown', startConnection);
        
        // Select node on click
        nodeElement.addEventListener('click', function(e) {
            e.stopPropagation();
            selectNode(node.id);
        });
    }

    /**
     * Make nodes draggable
     */
    let draggedNode = null;
    let offsetX, offsetY;

    function startDragNode(e) {
        e.stopPropagation();
        
        draggedNode = this;
        const rect = draggedNode.getBoundingClientRect();
        offsetX = e.clientX - rect.left;
        offsetY = e.clientY - rect.top;
        
        document.addEventListener('mousemove', dragNode);
        document.addEventListener('mouseup', stopDragNode);
    }

    function dragNode(e) {
        if (!draggedNode) return;
        
        const canvasRect = canvas.getBoundingClientRect();
        const x = e.clientX - canvasRect.left - offsetX;
        const y = e.clientY - canvasRect.top - offsetY;
        
        draggedNode.style.left = `${x}px`;
        draggedNode.style.top = `${y}px`;
        
        // Update node position in state
        const nodeId = draggedNode.id;
        const node = workflow.nodes.find(n => n.id === nodeId);
        if (node) {
            node.position.x = x + 60;
            node.position.y = y + 30;
        }
        
        // Update any connections
        updateConnections();
    }

    function stopDragNode() {
        draggedNode = null;
        document.removeEventListener('mousemove', dragNode);
        document.removeEventListener('mouseup', stopDragNode);
    }

    /**
     * Handle creating connections between nodes
     */
    let activeConnector = null;
    let tempLine = null;

    function startConnection(e) {
        e.stopPropagation();
        e.preventDefault();
        
        activeConnector = this;
        activeConnector.classList.add('active');
        
        // Create temporary SVG line
        if (!tempLine) {
            tempLine = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
            tempLine.style.position = 'absolute';
            tempLine.style.top = '0';
            tempLine.style.left = '0';
            tempLine.style.width = '100%';
            tempLine.style.height = '100%';
            tempLine.style.pointerEvents = 'none';
            tempLine.style.zIndex = '5';
            tempLine.innerHTML = '<path class="connection" d=""/>';
            canvas.appendChild(tempLine);
        }
        
        document.addEventListener('mousemove', dragConnection);
        document.addEventListener('mouseup', releaseConnection);
    }

    function dragConnection(e) {
        if (!activeConnector) return;
        
        const canvasRect = canvas.getBoundingClientRect();
        const connectorRect = activeConnector.getBoundingClientRect();
        
        const x1 = connectorRect.left + connectorRect.width/2 - canvasRect.left;
        const y1 = connectorRect.top + connectorRect.height/2 - canvasRect.top;
        const x2 = e.clientX - canvasRect.left;
        const y2 = e.clientY - canvasRect.top;
        
        // Draw bezier curve
        const path = tempLine.querySelector('path');
        const dx = Math.abs(x2 - x1) * 0.5;
        path.setAttribute('d', `M ${x1} ${y1} C ${x1} ${y1 + dx}, ${x2} ${y2 - dx}, ${x2} ${y2}`);
    }

    function releaseConnection(e) {
        if (!activeConnector) return;
        
        const canvasRect = canvas.getBoundingClientRect();
        const mouseX = e.clientX;
        const mouseY = e.clientY;
        
        // Find if mouse is over a connector
        const connectors = document.querySelectorAll('.connector:not(.active)');
        let targetConnector = null;
        
        connectors.forEach(connector => {
            const rect = connector.getBoundingClientRect();
            if (
                mouseX >= rect.left && mouseX <= rect.right &&
                mouseY >= rect.top && mouseY <= rect.bottom
            ) {
                targetConnector = connector;
            }
        });
        
        if (targetConnector) {
            const sourceConnector = activeConnector;
            const targetNode = targetConnector.getAttribute('data-node');
            const sourceNode = sourceConnector.getAttribute('data-node');
            const sourceType = sourceConnector.getAttribute('data-connector');
            const targetType = targetConnector.getAttribute('data-connector');
            
            // Only allow output -> input connections
            if (sourceType === 'output' && targetType === 'input') {
                createConnection(sourceNode, targetNode);
            }
        }
        
        // Clean up
        activeConnector.classList.remove('active');
        activeConnector = null;
        
        if (tempLine) {
            tempLine.innerHTML = '';
        }
        
        document.removeEventListener('mousemove', dragConnection);
        document.removeEventListener('mouseup', releaseConnection);
    }

    function createConnection(sourceNodeId, targetNodeId) {
        // Prevent duplicate connections
        const existingConnection = workflow.connections.find(
            conn => conn.sourceNodeId === sourceNodeId && conn.targetNodeId === targetNodeId
        );
        
        if (existingConnection) return;
        
        const connection = {
            id: `conn-${workflow.nextConnectionId++}`,
            sourceNodeId,
            targetNodeId
        };
        
        workflow.connections.push(connection);
        renderConnections();
    }

    function renderConnections() {
        // Clear existing connections
        const existingSvgs = canvas.querySelectorAll('svg.connection-line');
        existingSvgs.forEach(svg => svg.remove());
        
        // Create new SVG for all connections
        const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        svg.classList.add('connection-line');
        svg.style.position = 'absolute';
        svg.style.top = '0';
        svg.style.left = '0';
        svg.style.width = '100%';
        svg.style.height = '100%';
        svg.style.pointerEvents = 'none';
        svg.style.zIndex = '5';
        
        workflow.connections.forEach(connection => {
            const sourceNode = document.getElementById(connection.sourceNodeId);
            const targetNode = document.getElementById(connection.targetNodeId);
            
            if (!sourceNode || !targetNode) return;
            
            const sourceConnector = sourceNode.querySelector('.output-connector');
            const targetConnector = targetNode.querySelector('.input-connector');
            
            const sourceRect = sourceConnector.getBoundingClientRect();
            const targetRect = targetConnector.getBoundingClientRect();
            const canvasRect = canvas.getBoundingClientRect();
            
            const x1 = sourceRect.left + sourceRect.width/2 - canvasRect.left;
            const y1 = sourceRect.top + sourceRect.height/2 - canvasRect.top;
            const x2 = targetRect.left + targetRect.width/2 - canvasRect.left;
            const y2 = targetRect.top + targetRect.height/2 - canvasRect.top;
            
            // Draw bezier curve
            const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
            path.classList.add('connection');
            
            // Curved path with control points
            const dx = Math.abs(x2 - x1) * 0.5;
            path.setAttribute('d', `M ${x1} ${y1} C ${x1} ${y1 + dx}, ${x2} ${y2 - dx}, ${x2} ${y2}`);
            
            svg.appendChild(path);
        });
        
        canvas.appendChild(svg);
    }

    function updateConnections() {
        renderConnections();
    }

    /**
     * Handle node selection and properties panel
     */
    function selectNode(nodeId) {
        // Deselect previously selected node
        const prevSelected = document.querySelector('.node.selected');
        if (prevSelected) {
            prevSelected.classList.remove('selected');
        }
        
        // Select new node
        const nodeElement = document.getElementById(nodeId);
        if (nodeElement) {
            nodeElement.classList.add('selected');
            workflow.selectedNode = workflow.nodes.find(n => n.id === nodeId);
            showNodeProperties(workflow.selectedNode);
        }
    }

    function showNodeProperties(node) {
        if (!node) {
            document.querySelector('.select-node-msg').classList.remove('hidden');
            propertyForm.classList.add('hidden');
            return;
        }
        
        document.querySelector('.select-node-msg').classList.add('hidden');
        propertyForm.classList.remove('hidden');
        
        nodeLabel.value = node.label || '';
        nodeConfig.value = JSON.stringify(node.config || {}, null, 2);
    }

    function applyNodeProperties() {
        if (!workflow.selectedNode) return;
        
        workflow.selectedNode.label = nodeLabel.value;
        
        try {
            workflow.selectedNode.config = JSON.parse(nodeConfig.value);
        } catch (e) {
            showNotification('Invalid JSON in configuration', 'error');
            return;
        }
        
        // Update node display
        const nodeElement = document.getElementById(workflow.selectedNode.id);
        nodeElement.querySelector('.node-title').textContent = workflow.selectedNode.label;
        
        showNotification('Node properties updated', 'success');
    }

    function handleCanvasClick() {
        workflow.selectedNode = null;
        const selectedNode = document.querySelector('.node.selected');
        if (selectedNode) {
            selectedNode.classList.remove('selected');
        }
        
        showNodeProperties(null);
    }

    /**
     * Execute workflow
     */
    function executeWorkflow() {
        if (workflow.nodes.length === 0) {
            showNotification('Please add at least one node to the workflow', 'error');
            return;
        }
        
        // Show loading state
        setExecutionState('running');
        executionStatus.textContent = 'Preparing workflow execution...';
        executionResults.textContent = '';
        loadingIndicator.classList.remove('hidden');
        progressContainer.classList.remove('hidden');
        
        // Initialize progress with an animated effect that shows activity even if progress is 0
        startProgressAnimation();
        
        // Update UI
        executeButton.classList.add('hidden');
        cancelButton.classList.remove('hidden');
        
        // Convert workflow to backend format
        const backendWorkflow = {
            id: 'web-workflow',
            name: 'Web Workflow',
            description: 'Workflow created in web editor',
            nodes: workflow.nodes.map(node => ({
                id: node.id,
                nodeType: node.type,
                label: node.label,
                configuration: node.config,
                position: {
                    x: node.position.x,
                    y: node.position.y
                }
            })),
            connections: workflow.connections.map(conn => ({
                id: conn.id,
                sourceNodeId: conn.sourceNodeId,
                targetNodeId: conn.targetNodeId
            }))
        };
        
        // Call API to execute workflow
        fetch(getApiUrl('/api/workflow/execute'), {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                workflow: backendWorkflow,
                input: workflowInput.value
            })
        })
        .then(response => {
            if (!response.ok) {
                throw new Error(`Network error: ${response.status} ${response.statusText}`);
            }
            return response.json();
        })
        .then(data => {
            workflow.currentExecutionId = data.id;
            executionStatus.textContent = 'Workflow execution started. ID: ' + data.id;
            
            // Use WebSocket for real-time updates with debugging
            connectToWebSocket(data.id);
            
            // Add to execution history
            addToHistory(data.id, 'running', new Date());
            
            // We'll use the WebSocket for updates, but if it fails,
            // the WebSocket's onerror handler will fall back to polling
        })
        .catch(error => {
            setExecutionState('failed');
            executionStatus.textContent = 'Execution failed to start';
            executionResults.textContent = 'Error: ' + error.message;
            loadingIndicator.classList.add('hidden');
            showNotification('Failed to start workflow: ' + error.message, 'error');
            stopProgressAnimation();
        });
    }
    
    // Progress animation variables
    let progressAnimationInterval = null;
    let fakeProgress = 0;
    let lastRealProgress = 0;
    let progressIncreasing = true;
    
    /**
     * Start animated progress bar that shows activity even if progress is stuck at 0
     */
    function startProgressAnimation() {
        // Reset progress
        fakeProgress = 10;
        lastRealProgress = 0;
        progressFill.style.width = '10%';
        progressPercentage.textContent = '10%';
        
        // Clear any existing animation
        if (progressAnimationInterval) {
            clearInterval(progressAnimationInterval);
        }
        
        // Start animation that shows activity even if progress is 0
        progressAnimationInterval = setInterval(() => {
            // If we haven't received real progress updates, use a pulsing animation
            if (lastRealProgress === 0) {
                if (progressIncreasing) {
                    fakeProgress += 1;
                    if (fakeProgress >= 30) {
                        progressIncreasing = false;
                    }
                } else {
                    fakeProgress -= 1;
                    if (fakeProgress <= 10) {
                        progressIncreasing = true;
                    }
                }
                
                // Update progress bar with fake progress
                progressFill.style.width = `${fakeProgress}%`;
                progressPercentage.textContent = 'Processing...';
            }
        }, 200);
    }
    
    /**
     * Stop progress animation
     */
    function stopProgressAnimation() {
        if (progressAnimationInterval) {
            clearInterval(progressAnimationInterval);
            progressAnimationInterval = null;
        }
    }
    
    /**
     * Cancel a running workflow
     */
    function cancelWorkflow() {
        if (!workflow.currentExecutionId) return;
        
        executionStatus.textContent = 'Cancelling workflow...';
        
        fetch(getApiUrl(`/api/workflow/cancel/${workflow.currentExecutionId}`), {
            method: 'POST'
        })
        .then(response => {
            if (!response.ok) {
                throw new Error(`Network error: ${response.status} ${response.statusText}`);
            }
            
            executionStatus.textContent = 'Workflow cancelled';
            setExecutionState('cancelled');
            updateHistoryItem(workflow.currentExecutionId, 'cancelled');
            showNotification('Workflow cancelled successfully', 'warning');
            stopProgressAnimation();
        })
        .catch(error => {
            showNotification('Failed to cancel workflow: ' + error.message, 'error');
        });
    }
    
    /**
     * Set execution state and update UI
     */
    function setExecutionState(state) {
        // Update progress bar
        progressFill.className = `progress-fill status-${state}`;
        
        // Update buttons
        if (state === 'running') {
            executeButton.classList.add('hidden');
            cancelButton.classList.remove('hidden');
        } else {
            executeButton.classList.remove('hidden');
            cancelButton.classList.add('hidden');
        }
        
        // Hide loading indicator for completed states
        if (state === 'completed' || state === 'failed' || state === 'cancelled') {
            loadingIndicator.classList.add('hidden');
            stopProgressAnimation();
        }
    }
    
    /**
     * Connect to WebSocket for real-time updates with enhanced debugging
     */
    let webSocket = null;
    let wsDebugElement = null;
    
    // Create WebSocket debug panel
    function createWebSocketDebugPanel() {
        // Only create if it doesn't exist
        if (document.getElementById('ws-debug-panel')) return;
        
        // Create debug panel
        const debugPanel = document.createElement('div');
        debugPanel.id = 'ws-debug-panel';
        debugPanel.style.position = 'fixed';
        debugPanel.style.bottom = '0';
        debugPanel.style.right = '0';
        debugPanel.style.width = '400px';
        debugPanel.style.height = '300px';
        debugPanel.style.backgroundColor = 'rgba(0,0,0,0.8)';
        debugPanel.style.color = '#0f0';
        debugPanel.style.fontFamily = 'monospace';
        debugPanel.style.fontSize = '12px';
        debugPanel.style.padding = '10px';
        debugPanel.style.overflowY = 'scroll';
        debugPanel.style.zIndex = '9999';
        debugPanel.style.border = '1px solid #0f0';
        debugPanel.style.borderRadius = '5px 0 0 0';
        
        // Add header with close button
        debugPanel.innerHTML = `
            <div style="display: flex; justify-content: space-between; margin-bottom: 10px;">
                <span>WebSocket Debug</span>
                <button id="ws-debug-close" style="background: none; border: none; color: #0f0; cursor: pointer;">×</button>
            </div>
            <div id="ws-debug-content"></div>
        `;
        
        document.body.appendChild(debugPanel);
        
        // Set up close button
        document.getElementById('ws-debug-close').addEventListener('click', () => {
            debugPanel.style.display = 'none';
        });
        
        wsDebugElement = document.getElementById('ws-debug-content');
        
        // Add toggle button to UI for showing/hiding debug panel
        const toggleButton = document.createElement('button');
        toggleButton.id = 'ws-debug-toggle';
        toggleButton.innerText = 'Show WS Debug';
        toggleButton.style.position = 'fixed';
        toggleButton.style.bottom = '10px';
        toggleButton.style.right = '10px';
        toggleButton.style.zIndex = '9998';
        toggleButton.style.padding = '5px 10px';
        toggleButton.style.backgroundColor = '#333';
        toggleButton.style.color = '#0f0';
        toggleButton.style.border = '1px solid #0f0';
        toggleButton.style.borderRadius = '3px';
        toggleButton.style.cursor = 'pointer';
        
        toggleButton.addEventListener('click', () => {
            debugPanel.style.display = debugPanel.style.display === 'none' ? 'block' : 'none';
        });
        
        document.body.appendChild(toggleButton);
    }
    
    // Log to debug panel
    function wsDebugLog(message, type = 'info') {
        if (!wsDebugElement) return;
        
        const timestamp = new Date().toISOString().substring(11, 23);
        const colors = {
            info: '#0f0',
            error: '#f00',
            warning: '#ff0',
            receive: '#0ff',
            send: '#f0f'
        };
        
        const logItem = document.createElement('div');
        logItem.style.color = colors[type] || colors.info;
        logItem.style.marginBottom = '3px';
        logItem.innerHTML = `<span style="opacity: 0.7;">[${timestamp}]</span> ${message}`;
        
        wsDebugElement.appendChild(logItem);
        wsDebugElement.scrollTop = wsDebugElement.scrollHeight;
        
        // Also log to console
        console.log(`[WS ${type.toUpperCase()}] ${message}`);
    }
    
    function connectToWebSocket(workflowId) {
        // Initialize debug panel
        createWebSocketDebugPanel();
        wsDebugLog('Initializing WebSocket connection...');
        
        // Close any existing connection
        if (webSocket) {
            wsDebugLog('Closing existing WebSocket connection', 'warning');
            webSocket.close();
        }
        
        // Create new WebSocket connection
        // Always connect WebSockets to the ZIO HTTP server on port 8080
        const wsBaseUrl = apiBaseUrl.replace(/^http:\/\//, 'ws://').replace(/^https:\/\//, 'wss://');
        const wsUrl = `${wsBaseUrl}/api/workflow/ws/${workflowId}`;
        
        wsDebugLog(`Connecting to WebSocket: ${wsUrl}`);
        
        try {
            webSocket = new WebSocket(wsUrl);
            
            webSocket.onopen = () => {
                wsDebugLog('WebSocket connection established ✓', 'info');
                
                // Try sending a ping message to test the connection
                try {
                    webSocket.send(JSON.stringify({ type: 'ping', timestamp: Date.now() }));
                    wsDebugLog('Sent: ping message', 'send');
                } catch (e) {
                    wsDebugLog(`Failed to send ping: ${e.message}`, 'error');
                }
            };
            
            webSocket.onmessage = (event) => {
                const rawData = event.data;
                wsDebugLog(`Received raw data: ${rawData}`, 'receive');
                
                try {
                    const data = JSON.parse(rawData);
                    wsDebugLog(`Parsed data: ${JSON.stringify(data)}`, 'receive');
                    
                    // Add the data to the debug panel in a formatted way
                    if (data.progress !== undefined) {
                        wsDebugLog(`Progress update: ${data.progress}%`, 'info');
                    }
                    
                    updateWorkflowStatus(data);
                    
                    // If workflow is completed or failed, get the result
                    if (data.status === 'completed' || data.status === 'failed') {
                        wsDebugLog(`Workflow ${data.status}, fetching result`, 'info');
                        fetchWorkflowResult(workflowId);
                        updateHistoryItem(workflowId, data.status);
                    }
                } catch (e) {
                    wsDebugLog(`Error parsing WebSocket message: ${e.message}`, 'error');
                    wsDebugLog(`Raw message: ${rawData}`, 'error');
                }
            };
            
            webSocket.onerror = (error) => {
                wsDebugLog(`WebSocket error: ${error.message || 'Unknown error'}`, 'error');
                
                // Try to get more error details
                if (error.target) {
                    wsDebugLog(`WebSocket readyState: ${error.target.readyState}`, 'error');
                }
                
                // Fall back to polling on WebSocket error
                wsDebugLog('Falling back to polling mechanism', 'warning');
                pollWorkflowStatus(workflowId);
            };
            
            webSocket.onclose = (event) => {
                const codes = {
                    1000: 'Normal closure',
                    1001: 'Going away',
                    1002: 'Protocol error',
                    1003: 'Unsupported data',
                    1005: 'No status received',
                    1006: 'Abnormal closure',
                    1007: 'Invalid frame payload data',
                    1008: 'Policy violation',
                    1009: 'Message too big',
                    1010: 'Missing extension',
                    1011: 'Internal error',
                    1012: 'Service restart',
                    1013: 'Try again later',
                    1014: 'Bad gateway',
                    1015: 'TLS handshake'
                };
                
                const reason = event.reason || 'No reason provided';
                const code = event.code;
                const codeExplanation = codes[code] || 'Unknown';
                
                wsDebugLog(`WebSocket closed: Code ${code} (${codeExplanation}), Reason: ${reason}`, 'warning');
                
                // If closed unexpectedly, fall back to polling
                if (code !== 1000) {
                    wsDebugLog('Abnormal closure - falling back to polling', 'warning');
                    pollWorkflowStatus(workflowId);
                }
            };
        } catch (e) {
            wsDebugLog(`Failed to create WebSocket: ${e.message}`, 'error');
            pollWorkflowStatus(workflowId);
        }
    }
    
    /**
     * Poll for workflow status (fallback if WebSocket fails)
     */
    let currentPollInterval = null; // Track the current polling interval
    let progressStuckCount = 0; // Track how many times progress is stuck at same value
    let lastProgressValue = 0; // Track last progress value to detect stuck progress

    function pollWorkflowStatus(workflowId) {
        // Clear any existing poll interval
        if (currentPollInterval) {
            clearInterval(currentPollInterval);
        }
        
        // Start a new polling interval
        currentPollInterval = setInterval(() => {
            fetch(getApiUrl(`/api/workflow/status/${workflowId}`))
            .then(response => {
                if (!response.ok) {
                    throw new Error(`Failed to get workflow status: ${response.status} ${response.statusText}`);
                }
                return response.json();
            })
            .then(data => {
                // Check if the progress value has changed
                if (data.progress === lastProgressValue) {
                    progressStuckCount++;
                    
                    // If progress is stuck at 0 for too long, start advancing it artificially
                    if (data.progress === 0 && progressStuckCount > 3) {
                        // Use artificial progress value but keep track of server state
                        const artificialProgress = Math.min(25 + (progressStuckCount * 5), 90);
                        data.artificialProgress = artificialProgress;
                    }
                } else {
                    // Progress changed, reset counter
                    progressStuckCount = 0;
                    lastProgressValue = data.progress;
                }
                
                updateWorkflowStatus(data);
                
                // If workflow is completed or failed, get the result and stop polling
                if (data.status === 'completed' || data.status === 'failed' || data.status === 'cancelled') {
                    fetchWorkflowResult(workflowId);
                    clearInterval(currentPollInterval);
                    updateHistoryItem(workflowId, data.status);
                    currentPollInterval = null;
                }
            })
            .catch(error => {
                console.error('Error polling workflow status', error);
                // Don't clear the interval on error, keep trying
                showNotification('Error getting workflow status, retrying...', 'warning');
            });
        }, 2000); // Poll every 2 seconds
    }
    
    /**
     * Update the UI with workflow status
     */
    function updateWorkflowStatus(statusData) {
        // Stop animation when we get real progress
        if (statusData.progress > 0 || statusData.artificialProgress) {
            stopProgressAnimation();
        }
        
        // Use artificial progress if provided (for stuck at 0 case)
        const progressValue = statusData.artificialProgress || statusData.progress;
        
        // Set status text with proper formatting
        const statusText = statusData.status.charAt(0).toUpperCase() + statusData.status.slice(1);
        
        // Show artificial progress indicator if we're using it
        if (statusData.artificialProgress && statusData.progress === 0) {
            executionStatus.textContent = `Status: ${statusText} (Estimating progress...)`;
        } else {
            executionStatus.textContent = `Status: ${statusText} (${progressValue}%)`;
        }
        
        // Update progress bar
        progressFill.style.width = `${progressValue}%`;
        
        // Update progress text
        if (statusData.artificialProgress && statusData.progress === 0) {
            progressPercentage.textContent = 'Processing...';
        } else {
            progressPercentage.textContent = `${progressValue}%`;
        }
        
        // Store the real progress value
        lastRealProgress = statusData.progress;
        
        // Add status-specific styling
        progressFill.className = `progress-fill status-${statusData.status}`;
        
        // Update UI based on status
        setExecutionState(statusData.status);
    }
    
    /**
     * Fetch the result of a workflow execution
     */
    function fetchWorkflowResult(workflowId) {
        fetch(getApiUrl(`/api/workflow/result/${workflowId}`))
        .then(response => {
            if (!response.ok) {
                if (response.status === 400) {
                    // Workflow is not completed yet or has no result
                    return response.text().then(text => {
                        executionResults.textContent = text;
                        return null;
                    });
                }
                throw new Error(`Failed to get workflow result: ${response.status} ${response.statusText}`);
            }
            return response.json();
        })
        .then(data => {
            if (data) {
                executionResults.textContent = data.result;
                showNotification('Workflow execution completed', 'success');
            }
        })
        .catch(error => {
            executionResults.textContent = 'Error retrieving result: ' + error.message;
            showNotification('Error getting workflow result: ' + error.message, 'error');
        });
    }

    /**
     * Save and load workflows
     */
    function saveWorkflow() {
        if (workflow.nodes.length === 0) {
            showNotification('Nothing to save. Add some nodes first.', 'warning');
            return;
        }
        
        const workflowData = JSON.stringify(workflow);
        localStorage.setItem('savedWorkflow', workflowData);
        showNotification('Workflow saved successfully!', 'success');
    }

    function loadWorkflow(workflowData) {
        // Clear current state
        workflow.nodes.forEach(node => {
            const element = document.getElementById(node.id);
            if (element) element.remove();
        });
        
        workflow = workflowData;
        
        // Make sure IDs don't overlap
        workflow.nextNodeId = Math.max(workflow.nextNodeId, findHighestNodeId() + 1);
        workflow.nextConnectionId = Math.max(workflow.nextConnectionId, findHighestConnectionId() + 1);
        
        // Render loaded workflow
        workflow.nodes.forEach(node => renderNode(node));
        renderConnections();
        
        showNotification('Workflow loaded successfully!', 'success');
    }

    function findHighestNodeId() {
        if (workflow.nodes.length === 0) return 0;
        return Math.max(...workflow.nodes.map(n => {
            const idNum = parseInt(n.id.replace('node-', ''));
            return isNaN(idNum) ? 0 : idNum;
        }));
    }

    function findHighestConnectionId() {
        if (workflow.connections.length === 0) return 0;
        return Math.max(...workflow.connections.map(c => {
            const idNum = parseInt(c.id.replace('conn-', ''));
            return isNaN(idNum) ? 0 : idNum;
        }));
    }

    function loadExampleWorkflow() {
        const exampleWorkflow = {
            nodes: [
                {
                    id: 'node-1',
                    type: 'text-transformer',
                    label: 'Capitalize Text',
                    config: { 'transform': 'capitalize' },
                    position: { x: 150, y: 100 }
                },
                {
                    id: 'node-2',
                    type: 'text-splitter',
                    label: 'Split Text',
                    config: { 'delimiter': '.' },
                    position: { x: 350, y: 100 }
                },
                {
                    id: 'node-3',
                    type: 'summarizer',
                    label: 'Summarize Text',
                    config: {},
                    position: { x: 550, y: 100 }
                }
            ],
            connections: [
                {
                    id: 'conn-1',
                    sourceNodeId: 'node-1',
                    targetNodeId: 'node-2'
                },
                {
                    id: 'conn-2',
                    sourceNodeId: 'node-2',
                    targetNodeId: 'node-3'
                }
            ],
            nextNodeId: 4,
            nextConnectionId: 3,
            selectedNode: null,
            currentExecutionId: null
        };
        
        loadWorkflow(exampleWorkflow);
    }

    /**
     * Workflow History Management
     */
    function addToHistory(id, status, timestamp) {
        // Show history panel if hidden
        workflowHistory.classList.remove('hidden');
        
        // Create history item
        const historyItem = document.createElement('div');
        historyItem.className = 'history-item';
        historyItem.id = `history-${id}`;
        historyItem.dataset.id = id;
        
        historyItem.innerHTML = `
            <div class="time">${timestamp.toLocaleTimeString()}</div>
            <div class="status">
                <span class="status-badge ${status}">${status}</span>
                <span>Workflow ID: ${id.substring(0, 8)}...</span>
            </div>
        `;
        
        // Add click event to load this workflow execution
        historyItem.addEventListener('click', () => {
            fetch(getApiUrl(`/api/workflow/status/${id}`))
                .then(response => response.json())
                .then(data => {
                    updateWorkflowStatus(data);
                    fetchWorkflowResult(id);
                    workflow.currentExecutionId = id;
                });
        });
        
        // Add to history list
        historyList.prepend(historyItem);
        
        // Limit history items to 10
        const historyItems = historyList.querySelectorAll('.history-item');
        if (historyItems.length > 10) {
            historyList.removeChild(historyItems[historyItems.length - 1]);
        }
    }
    
    function updateHistoryItem(id, status) {
        const historyItem = document.getElementById(`history-${id}`);
        if (historyItem) {
            const statusBadge = historyItem.querySelector('.status-badge');
            statusBadge.className = `status-badge ${status}`;
            statusBadge.textContent = status;
        }
    }
    
    /**
     * Show notification
     */
    function showNotification(message, type) {
        // Check if notification container exists, if not create it
        let notificationContainer = document.getElementById('notification-container');
        if (!notificationContainer) {
            notificationContainer = document.createElement('div');
            notificationContainer.id = 'notification-container';
            notificationContainer.style.position = 'fixed';
            notificationContainer.style.top = '20px';
            notificationContainer.style.right = '20px';
            notificationContainer.style.zIndex = '1000';
            document.body.appendChild(notificationContainer);
        }
        
        // Create notification element
        const notification = document.createElement('div');
        notification.className = `notification ${type}`;
        notification.style.backgroundColor = type === 'success' ? '#2ecc71' : 
                                            type === 'error' ? '#e74c3c' : 
                                            type === 'warning' ? '#f39c12' : '#3498db';
        notification.style.color = 'white';
        notification.style.padding = '12px 20px';
        notification.style.marginBottom = '10px';
        notification.style.borderRadius = '4px';
        notification.style.boxShadow = '0 2px 10px rgba(0,0,0,0.1)';
        notification.style.display = 'flex';
        notification.style.justifyContent = 'space-between';
        notification.style.alignItems = 'center';
        notification.style.animation = 'slideInFromRight 0.3s forwards';
        
        // Add icon based on type
        let icon = '';
        switch (type) {
            case 'success': icon = '✓'; break;
            case 'error': icon = '✕'; break;
            case 'warning': icon = '⚠'; break;
            default: icon = 'ℹ';
        }
        
        notification.innerHTML = `
            <div style="display: flex; align-items: center;">
                <span style="margin-right: 10px; font-size: 18px;">${icon}</span>
                <span>${message}</span>
            </div>
            <button style="background: none; border: none; color: white; cursor: pointer; font-size: 18px; margin-left: 10px;">×</button>
        `;
        
        // Add close button functionality
        const closeButton = notification.querySelector('button');
        closeButton.addEventListener('click', () => {
            notification.style.animation = 'slideOutToRight 0.3s forwards';
            setTimeout(() => {
                notification.remove();
            }, 300);
        });
        
        // Add to container
        notificationContainer.appendChild(notification);
        
        // Auto remove after 5 seconds
        setTimeout(() => {
            if (notification.parentNode) {
                notification.style.animation = 'slideOutToRight 0.3s forwards';
                setTimeout(() => {
                    if (notification.parentNode) {
                        notification.remove();
                    }
                }, 300);
            }
        }, 5000);
        
        // Add CSS animations if they don't exist
        if (!document.getElementById('notification-styles')) {
            const styleSheet = document.createElement('style');
            styleSheet.id = 'notification-styles';
            styleSheet.textContent = `
                @keyframes slideInFromRight {
                    0% { transform: translateX(100%); opacity: 0; }
                    100% { transform: translateX(0); opacity: 1; }
                }
                @keyframes slideOutToRight {
                    0% { transform: translateX(0); opacity: 1; }
                    100% { transform: translateX(100%); opacity: 0; }
                }
            `;
            document.head.appendChild(styleSheet);
        }
    }

    // Load saved workflow if available
    const savedWorkflow = localStorage.getItem('savedWorkflow');
    if (savedWorkflow) {
        try {
            loadWorkflow(JSON.parse(savedWorkflow));
        } catch (e) {
            console.error('Error loading saved workflow', e);
            loadExampleWorkflow();
        }
    } else {
        // Load example workflow by default
        loadExampleWorkflow();
    }
    
    // Add UI for changing API base URL when in file:// mode
    if (isFileProtocol) {
        const settingsContainer = document.createElement('div');
        settingsContainer.className = 'api-settings';
        settingsContainer.style.position = 'fixed';
        settingsContainer.style.bottom = '10px';
        settingsContainer.style.right = '10px';
        settingsContainer.style.background = '#f8f9fa';
        settingsContainer.style.padding = '10px';
        settingsContainer.style.border = '1px solid #ddd';
        settingsContainer.style.borderRadius = '4px';
        settingsContainer.style.zIndex = '1000';
        
        settingsContainer.innerHTML = `
            <h4 style="margin-top: 0; margin-bottom: 5px;">API Settings</h4>
            <div style="display: flex; align-items: center;">
                <input type="text" id="api-base-url" value="${apiBaseUrl}"
                       style="flex: 1; margin-right: 5px; padding: 5px;"
                       placeholder="http://localhost:8080">
                <button id="save-api-url" style="padding: 5px 10px;">Save</button>
            </div>
            <small style="display: block; margin-top: 5px; color: #666;">
                API base URL for file:// mode
            </small>
        `;
        
        document.body.appendChild(settingsContainer);
        
        // Add event listener for the save button
        document.getElementById('save-api-url').addEventListener('click', function() {
            const newBaseUrl = document.getElementById('api-base-url').value.trim();
            if (newBaseUrl) {
                localStorage.setItem('workflowApiBaseUrl', newBaseUrl);
                apiBaseUrl = newBaseUrl;
                showNotification('API base URL updated. Refresh the page for changes to take effect.', 'success');
            } else {
                showNotification('Please enter a valid URL', 'error');
            }
        });
    }
});