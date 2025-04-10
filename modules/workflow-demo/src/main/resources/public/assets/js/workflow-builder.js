/**
 * Workflow Builder - Manages the visual workflow editing experience
 */
document.addEventListener('DOMContentLoaded', () => {
    // DOM Elements
    const canvas = document.getElementById('workflow-canvas');
    const nodeItems = document.querySelectorAll('.node-item');
    const newWorkflowBtn = document.getElementById('new-workflow-btn');
    const saveWorkflowBtn = document.getElementById('save-workflow-btn');
    const executeWorkflowBtn = document.getElementById('execute-workflow-btn');
    
    // State
    let nodes = [];
    let connections = [];
    let nextNodeId = 1;
    let draggingNode = null;
    let selectedNode = null;
    let connectingPort = null;
    let currentWorkflowId = null;
    
    // Node template
    const nodeTemplate = document.getElementById('workflow-node-template');
    
    // Initialize
    init();
    
    /**
     * Initializes the workflow builder
     */
    function init() {
        // Set up drag and drop for node palette
        nodeItems.forEach(nodeItem => {
            nodeItem.addEventListener('dragstart', onNodeItemDragStart);
        });
        
        // Set up canvas events
        canvas.addEventListener('dragover', onCanvasDragOver);
        canvas.addEventListener('drop', onCanvasDrop);
        canvas.addEventListener('mousedown', onCanvasMouseDown);
        canvas.addEventListener('mousemove', onCanvasMouseMove);
        canvas.addEventListener('mouseup', onCanvasMouseUp);
        
        // Buttons
        newWorkflowBtn.addEventListener('click', createNewWorkflow);
        saveWorkflowBtn.addEventListener('click', saveWorkflow);
        executeWorkflowBtn.addEventListener('click', executeWorkflow);
        
        // Load any existing workflow or create a default
        loadDefaultWorkflow();
    }
    
    /**
     * Creates a new workflow node from a template
     */
    function createNode(type, label, x, y, config = {}) {
        const nodeId = `node-${nextNodeId++}`;
        const nodeFragment = nodeTemplate.content.cloneNode(true);
        const nodeElement = nodeFragment.querySelector('.workflow-node');
        
        // Set up the node
        nodeElement.dataset.nodeId = nodeId;
        nodeElement.dataset.nodeType = type;
        nodeElement.style.left = `${x}px`;
        nodeElement.style.top = `${y}px`;
        
        // Set label
        const nodeLabel = nodeElement.querySelector('.node-label');
        nodeLabel.textContent = label;
        
        // Add configuration UI based on node type
        const nodeParams = nodeElement.querySelector('.node-params');
        setupNodeParams(nodeParams, type, config);
        
        // Event handling
        const nodePorts = nodeElement.querySelectorAll('.port');
        nodePorts.forEach(port => {
            port.addEventListener('mousedown', onPortMouseDown);
        });
        
        nodeElement.addEventListener('mousedown', onNodeMouseDown);
        
        const deleteBtn = nodeElement.querySelector('.node-delete-btn');
        deleteBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            deleteNode(nodeId);
        });
        
        // Add to canvas
        canvas.appendChild(nodeElement);
        
        // Add to internal state
        nodes.push({
            id: nodeId,
            type: type,
            label: label,
            position: { x, y },
            config: config
        });
        
        return nodeElement;
    }
    
    /**
     * Sets up node configuration parameters based on node type
     */
    function setupNodeParams(paramsContainer, nodeType, config) {
        paramsContainer.innerHTML = '';
        
        switch (nodeType) {
            case 'text-transformer':
                const transformOptions = ['capitalize', 'lowercase', 'uppercase'];
                const transformLabel = document.createElement('label');
                transformLabel.textContent = 'Transform Type:';
                
                const transformSelect = document.createElement('select');
                transformSelect.name = 'transform';
                transformSelect.className = 'node-param';
                
                transformOptions.forEach(option => {
                    const optEl = document.createElement('option');
                    optEl.value = option;
                    optEl.textContent = option.charAt(0).toUpperCase() + option.slice(1);
                    if (config.transform === option) {
                        optEl.selected = true;
                    }
                    transformSelect.appendChild(optEl);
                });
                
                paramsContainer.appendChild(transformLabel);
                paramsContainer.appendChild(transformSelect);
                break;
                
            case 'text-splitter':
                const delimiterLabel = document.createElement('label');
                delimiterLabel.textContent = 'Split by:';
                
                const delimiterInput = document.createElement('input');
                delimiterInput.type = 'text';
                delimiterInput.name = 'delimiter';
                delimiterInput.className = 'node-param';
                delimiterInput.value = config.delimiter || '\\n';
                
                paramsContainer.appendChild(delimiterLabel);
                paramsContainer.appendChild(delimiterInput);
                break;
                
            case 'summarizer':
                // Summarizer doesn't need configuration
                const infoText = document.createElement('p');
                infoText.textContent = 'Uses AI to summarize text';
                paramsContainer.appendChild(infoText);
                break;
        }
    }
    
    /**
     * Creates a connection between two nodes
     */
    function createConnection(sourceNodeId, targetNodeId) {
        const sourceNode = document.querySelector(`.workflow-node[data-node-id="${sourceNodeId}"]`);
        const targetNode = document.querySelector(`.workflow-node[data-node-id="${targetNodeId}"]`);
        
        if (!sourceNode || !targetNode) return;
        
        const sourcePort = sourceNode.querySelector('.output-port');
        const targetPort = targetNode.querySelector('.input-port');
        
        // Check if connection already exists
        const existingConnection = connections.find(
            c => c.sourceNodeId === sourceNodeId && c.targetNodeId === targetNodeId
        );
        
        if (existingConnection) return;
        
        // Create SVG line
        const line = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        line.classList.add('connection-line');
        line.style.position = 'absolute';
        line.style.pointerEvents = 'none';
        line.style.zIndex = '5';
        
        const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
        path.setAttribute('stroke', '#2c3e50');
        path.setAttribute('stroke-width', '2');
        path.setAttribute('fill', 'none');
        line.appendChild(path);
        
        // Add to canvas
        canvas.appendChild(line);
        
        // Add to state
        const connectionId = `conn-${sourceNodeId}-${targetNodeId}`;
        connections.push({
            id: connectionId,
            sourceNodeId,
            targetNodeId,
            element: line,
            path
        });
        
        // Update the connection visually
        updateConnection(connectionId);
    }
    
    /**
     * Updates the visual representation of a connection
     */
    function updateConnection(connectionId) {
        const connection = connections.find(c => c.id === connectionId);
        if (!connection) return;
        
        const sourceNode = document.querySelector(`.workflow-node[data-node-id="${connection.sourceNodeId}"]`);
        const targetNode = document.querySelector(`.workflow-node[data-node-id="${connection.targetNodeId}"]`);
        
        if (!sourceNode || !targetNode) return;
        
        const sourcePort = sourceNode.querySelector('.output-port');
        const targetPort = targetNode.querySelector('.input-port');
        
        const sourceRect = sourcePort.getBoundingClientRect();
        const targetRect = targetPort.getBoundingClientRect();
        const canvasRect = canvas.getBoundingClientRect();
        
        const x1 = sourceRect.left + sourceRect.width / 2 - canvasRect.left;
        const y1 = sourceRect.top + sourceRect.height / 2 - canvasRect.top;
        const x2 = targetRect.left + targetRect.width / 2 - canvasRect.left;
        const y2 = targetRect.top + targetRect.height / 2 - canvasRect.top;
        
        // Calculate bezier control points
        const dx = Math.abs(x2 - x1) * 0.5;
        
        // Update SVG dimensions
        connection.element.style.left = '0';
        connection.element.style.top = '0';
        connection.element.style.width = `${canvas.scrollWidth}px`;
        connection.element.style.height = `${canvas.scrollHeight}px`;
        
        // Update path
        connection.path.setAttribute('d', `M ${x1} ${y1} C ${x1 + dx} ${y1}, ${x2 - dx} ${y2}, ${x2} ${y2}`);
    }
    
    /**
     * Updates all connections in the workflow
     */
    function updateAllConnections() {
        connections.forEach(connection => {
            updateConnection(connection.id);
        });
    }
    
    /**
     * Deletes a node and its connections
     */
    function deleteNode(nodeId) {
        // Remove the node element
        const nodeElement = document.querySelector(`.workflow-node[data-node-id="${nodeId}"]`);
        if (nodeElement) {
            nodeElement.remove();
        }
        
        // Remove connections involving this node
        const nodeConnections = connections.filter(
            c => c.sourceNodeId === nodeId || c.targetNodeId === nodeId
        );
        
        nodeConnections.forEach(conn => {
            conn.element.remove();
            connections = connections.filter(c => c.id !== conn.id);
        });
        
        // Remove from state
        nodes = nodes.filter(n => n.id !== nodeId);
    }
    
    /**
     * Creates a new empty workflow
     */
    function createNewWorkflow() {
        if (confirm('Create new workflow? Current workflow will be lost.')) {
            // Clear state
            nodes.forEach(node => {
                const nodeElement = document.querySelector(`.workflow-node[data-node-id="${node.id}"]`);
                if (nodeElement) nodeElement.remove();
            });
            
            connections.forEach(conn => {
                conn.element.remove();
            });
            
            nodes = [];
            connections = [];
            nextNodeId = 1;
            currentWorkflowId = null;
            
            // Ensure input/output is cleared
            document.getElementById('input-text').value = '';
            document.getElementById('output-text').innerHTML = '';
        }
    }
    
    /**
     * Loads a default workflow into the builder
     */
    function loadDefaultWorkflow() {
        // Create nodes
        const node1 = createNode('text-transformer', 'Capitalize Text', 150, 100, { transform: 'capitalize' });
        const node2 = createNode('summarizer', 'Summarize Text', 450, 100, {});
        
        // Create connections
        createConnection('node-1', 'node-2');
    }
    
    /**
     * Saves the current workflow to the server
     */
    function saveWorkflow() {
        // Collect the current layout
        const workflow = {
            id: currentWorkflowId || null,
            name: 'My Workflow',
            description: 'A workflow created in the workflow builder',
            nodes: nodes.map(node => {
                // Collect the parameters from the DOM
                const nodeElement = document.querySelector(`.workflow-node[data-node-id="${node.id}"]`);
                const params = {};
                if (nodeElement) {
                    const paramInputs = nodeElement.querySelectorAll('.node-param');
                    paramInputs.forEach(input => {
                        params[input.name] = input.value;
                    });
                }
                
                return {
                    id: node.id,
                    nodeType: node.type,
                    label: node.label,
                    configuration: params,
                    position: node.position
                };
            }),
            connections: connections.map(conn => ({
                id: conn.id,
                sourceNodeId: conn.sourceNodeId,
                targetNodeId: conn.targetNodeId
            }))
        };
        
        // Send to server
        fetch('/api/workflows', {
            method: currentWorkflowId ? 'PUT' : 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(workflow)
        })
        .then(response => response.json())
        .then(data => {
            alert('Workflow saved successfully!');
            currentWorkflowId = data.id;
        })
        .catch(error => {
            console.error('Error saving workflow:', error);
            alert('Error saving workflow');
        });
    }
    
    /**
     * Executes the current workflow
     */
    function executeWorkflow() {
        const inputText = document.getElementById('input-text').value;
        const outputEl = document.getElementById('output-text');
        
        if (!inputText.trim()) {
            alert('Please enter some input text to process');
            return;
        }
        
        outputEl.innerHTML = 'Processing...';
        
        // Determine workflow ID to execute
        const workflowIdToExecute = currentWorkflowId || defaultWorkflow.id;
        
        // Execute workflow
        fetch(`/api/workflows/${workflowIdToExecute}/execute`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ input: inputText })
        })
        .then(response => response.json())
        .then(result => {
            outputEl.innerHTML = result;
        })
        .catch(error => {
            console.error('Error executing workflow:', error);
            outputEl.innerHTML = 'Error: ' + error.message;
            
            // For demo purposes, show mock result
            if (!currentWorkflowId) {
                mockExecuteWorkflow(inputText);
            }
        });
    }
    
    /**
     * Executes the workflow locally for demo purposes
     */
    function mockExecuteWorkflow(inputText) {
        const outputEl = document.getElementById('output-text');
        
        // Simple mock execution just to show functionality
        setTimeout(() => {
            let result = inputText;
            
            // Process through each node in order (very simplistic implementation)
            const nodeIds = [];
            connections.forEach(conn => {
                if (!nodeIds.includes(conn.sourceNodeId)) {
                    nodeIds.push(conn.sourceNodeId);
                }
                if (!nodeIds.includes(conn.targetNodeId)) {
                    nodeIds.push(conn.targetNodeId);
                }
            });
            
            // Apply each node's operation
            for (const nodeId of nodeIds) {
                const node = nodes.find(n => n.id === nodeId);
                if (!node) continue;
                
                switch (node.type) {
                    case 'text-transformer':
                        const nodeElement = document.querySelector(`.workflow-node[data-node-id="${nodeId}"]`);
                        const transformSelect = nodeElement.querySelector('select[name="transform"]');
                        const transformType = transformSelect ? transformSelect.value : 'capitalize';
                        
                        switch (transformType) {
                            case 'capitalize':
                                result = result.replace(/\b\w/g, c => c.toUpperCase());
                                break;
                            case 'uppercase':
                                result = result.toUpperCase();
                                break;
                            case 'lowercase':
                                result = result.toLowerCase();
                                break;
                        }
                        break;
                        
                    case 'text-splitter':
                        const delimiterInput = document.querySelector(`.workflow-node[data-node-id="${nodeId}"] input[name="delimiter"]`);
                        const delimiter = delimiterInput ? delimiterInput.value : '\\n';
                        result = result.split(delimiter).join('<br>');
                        break;
                        
                    case 'summarizer':
                        // Mock summarization
                        result = `<strong>Mock AI Summary:</strong><br><br>` + 
                                 result.split(/[.!?]/).slice(0, 3).join('. ') + '...';
                        break;
                }
            }
            
            outputEl.innerHTML = result;
        }, 1000);
    }
    
    // Event handlers
    function onNodeItemDragStart(e) {
        const nodeType = e.target.dataset.nodeType;
        e.dataTransfer.setData('nodeType', nodeType);
    }
    
    function onCanvasDragOver(e) {
        e.preventDefault();
    }
    
    function onCanvasDrop(e) {
        e.preventDefault();
        const nodeType = e.dataTransfer.getData('nodeType');
        if (!nodeType) return;
        
        const rect = canvas.getBoundingClientRect();
        const x = e.clientX - rect.left;
        const y = e.clientY - rect.top;
        
        let label = 'Node';
        switch (nodeType) {
            case 'text-transformer': label = 'Text Transformer'; break;
            case 'summarizer': label = 'Summarizer'; break;
            case 'text-splitter': label = 'Text Splitter'; break;
        }
        
        createNode(nodeType, label, x, y);
    }
    
    function onNodeMouseDown(e) {
        if (e.target.classList.contains('port')) return;
        e.stopPropagation();
        
        const node = e.currentTarget;
        selectedNode = node;
        
        const initialX = e.clientX;
        const initialY = e.clientY;
        const nodeRect = node.getBoundingClientRect();
        const offsetX = initialX - nodeRect.left;
        const offsetY = initialY - nodeRect.top;
        
        const onMouseMove = (e) => {
            const x = e.clientX - offsetX - canvas.getBoundingClientRect().left;
            const y = e.clientY - offsetY - canvas.getBoundingClientRect().top;
            
            node.style.left = `${x}px`;
            node.style.top = `${y}px`;
            
            // Update node state
            const nodeData = nodes.find(n => n.id === node.dataset.nodeId);
            if (nodeData) {
                nodeData.position = { x, y };
            }
            
            // Update connections
            updateAllConnections();
        };
        
        const onMouseUp = () => {
            document.removeEventListener('mousemove', onMouseMove);
            document.removeEventListener('mouseup', onMouseUp);
            selectedNode = null;
        };
        
        document.addEventListener('mousemove', onMouseMove);
        document.addEventListener('mouseup', onMouseUp);
    }
    
    function onPortMouseDown(e) {
        e.stopPropagation();
        
        const port = e.target;
        const node = port.closest('.workflow-node');
        const portType = port.dataset.portType;
        
        if (portType === 'output') {
            connectingPort = {
                type: 'output',
                nodeId: node.dataset.nodeId,
                element: port
            };
            
            const onMouseMove = (e) => {
                const canvasRect = canvas.getBoundingClientRect();
                const x1 = port.getBoundingClientRect().left + port.offsetWidth / 2 - canvasRect.left;
                const y1 = port.getBoundingClientRect().top + port.offsetHeight / 2 - canvasRect.top;
                const x2 = e.clientX - canvasRect.left;
                const y2 = e.clientY - canvasRect.top;
                
                // Calculate bezier curve control points
                const dx = Math.abs(x2 - x1) * 0.5;
                
                // Draw temporary connection line
                drawTemporaryConnection(x1, y1, x2, y2, dx);
            };
            
            const onMouseUp = (e) => {
                // Remove temporary connection
                const tempConnection = document.getElementById('temp-connection');
                if (tempConnection) tempConnection.remove();
                
                // Check if we're over an input port
                const targetPort = document.elementFromPoint(e.clientX, e.clientY);
                if (targetPort && targetPort.classList.contains('port') && 
                    targetPort.dataset.portType === 'input') {
                    
                    const targetNode = targetPort.closest('.workflow-node');
                    if (targetNode && targetNode.dataset.nodeId !== connectingPort.nodeId) {
                        // Create a connection
                        createConnection(connectingPort.nodeId, targetNode.dataset.nodeId);
                    }
                }
                
                document.removeEventListener('mousemove', onMouseMove);
                document.removeEventListener('mouseup', onMouseUp);
                connectingPort = null;
            };
            
            document.addEventListener('mousemove', onMouseMove);
            document.addEventListener('mouseup', onMouseUp);
        }
    }
    
    function onCanvasMouseDown(e) {
        // Handle background clicks if needed
    }
    
    function onCanvasMouseMove(e) {
        // Handle canvas mouse movement if needed
    }
    
    function onCanvasMouseUp(e) {
        // Handle canvas mouse up if needed
    }
    
    /**
     * Draws a temporary connection line while dragging
     */
    function drawTemporaryConnection(x1, y1, x2, y2, dx) {
        // Remove any existing temporary connection
        const existingTemp = document.getElementById('temp-connection');
        if (existingTemp) existingTemp.remove();
        
        // Create new SVG element
        const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        svg.id = 'temp-connection';
        svg.classList.add('connection-line');
        svg.style.position = 'absolute';
        svg.style.left = '0';
        svg.style.top = '0';
        svg.style.width = `${canvas.scrollWidth}px`;
        svg.style.height = `${canvas.scrollHeight}px`;
        svg.style.pointerEvents = 'none';
        svg.style.zIndex = '100';
        
        const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
        path.setAttribute('d', `M ${x1} ${y1} C ${x1 + dx} ${y1}, ${x2 - dx} ${y2}, ${x2} ${y2}`);
        path.setAttribute('stroke', '#3498db');
        path.setAttribute('stroke-width', '2');
        path.setAttribute('stroke-dasharray', '5,5');
        path.setAttribute('fill', 'none');
        
        svg.appendChild(path);
        canvas.appendChild(svg);
    }
});