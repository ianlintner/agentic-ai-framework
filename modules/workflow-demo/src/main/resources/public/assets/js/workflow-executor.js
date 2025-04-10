/**
 * Workflow Executor - Handles the workflow execution logic
 */
document.addEventListener('DOMContentLoaded', () => {
    // DOM Elements
    const executeBtn = document.getElementById('execute-workflow-btn');
    const inputTextArea = document.getElementById('input-text');
    const outputContainer = document.getElementById('output-text');
    
    // Initialize 
    init();
    
    /**
     * Initialize the executor
     */
    function init() {
        executeBtn.addEventListener('click', onExecuteWorkflow);
    }
    
    /**
     * Handle workflow execution
     */
    function onExecuteWorkflow() {
        const inputText = inputTextArea.value.trim();
        
        if (!inputText) {
            alert('Please enter some text to process');
            return;
        }
        
        // Show loading state
        outputContainer.innerHTML = '<div class="loading">Processing...</div>';
        
        // For demo purposes, do the client-side execution
        executeClientSide(inputText);
    }
    
    /**
     * Execute the workflow on the client side for demo purposes
     * In a real implementation, this would call the server API
     */
    function executeClientSide(input) {
        let currentText = input;
        const nodes = getNodesInExecutionOrder();
        
        // Process the input through each node sequentially
        processNextNode(0, nodes, currentText);
    }
    
    /**
     * Process nodes recursively to show step-by-step execution
     */
    function processNextNode(index, nodes, currentText) {
        if (index >= nodes.length) {
            // All nodes processed
            outputContainer.innerHTML = formatOutput(currentText);
            return;
        }
        
        const node = nodes[index];
        const nodeElement = document.querySelector(`.workflow-node[data-node-id="${node.id}"]`);
        
        // Highlight the current node being processed
        highlightNode(node.id);
        
        // Show intermediate result
        if (index > 0) {
            outputContainer.innerHTML = 
                `<div class="processing-step">
                    <div class="step-header">Processing node: ${node.label}</div>
                    <div class="step-input">${formatOutput(currentText)}</div>
                    <div class="step-output loading">Processing...</div>
                </div>`;
        }
        
        // Process the current node
        setTimeout(() => {
            const result = processNode(node.id, currentText);
            
            // Update the display
            if (index > 0) {
                outputContainer.querySelector('.step-output').innerHTML = formatOutput(result);
                outputContainer.querySelector('.step-output').classList.remove('loading');
            }
            
            // Continue to next node
            setTimeout(() => {
                unhighlightNode(node.id);
                processNextNode(index + 1, nodes, result);
            }, 500);
        }, 1000);
    }
    
    /**
     * Process a single node
     */
    function processNode(nodeId, input) {
        const nodeElement = document.querySelector(`.workflow-node[data-node-id="${nodeId}"]`);
        
        if (!nodeElement) return input;
        
        const nodeType = nodeElement.dataset.nodeType;
        const params = getNodeParameters(nodeElement);
        
        switch (nodeType) {
            case 'text-transformer':
                const transformType = params.transform || 'capitalize';
                
                switch (transformType) {
                    case 'capitalize':
                        return input.replace(/\b\w/g, c => c.toUpperCase());
                    case 'uppercase':
                        return input.toUpperCase();
                    case 'lowercase':
                        return input.toLowerCase();
                    default:
                        return input;
                }
                
            case 'text-splitter':
                const delimiter = params.delimiter || '\\n';
                // Split and join with HTML line breaks for display
                return input.split(delimiter).join('<br>');
                
            case 'summarizer':
                // Mock summarization for demo purposes
                const sentences = input.split(/[.!?]+/).filter(s => s.trim().length > 0);
                
                if (sentences.length <= 3) {
                    return `<strong>Summary:</strong><br>${input}`;
                }
                
                // Take first 2 sentences and add ellipsis
                const summary = sentences.slice(0, 2).join('. ') + '.';
                return `<strong>AI Summary:</strong><br><br>${summary}<br><br><em>(This is a mock summary - in a real application, this would use Claude or another LLM)</em>`;
                
            default:
                return input;
        }
    }
    
    /**
     * Get parameters for a specific node
     */
    function getNodeParameters(nodeElement) {
        const params = {};
        const paramInputs = nodeElement.querySelectorAll('.node-param');
        
        paramInputs.forEach(input => {
            params[input.name] = input.value;
        });
        
        return params;
    }
    
    /**
     * Get nodes in the correct execution order
     */
    function getNodesInExecutionOrder() {
        const nodeElements = document.querySelectorAll('.workflow-node');
        const nodes = Array.from(nodeElements).map(el => ({
            id: el.dataset.nodeId,
            type: el.dataset.nodeType,
            label: el.querySelector('.node-label').textContent
        }));
        
        const connectionElements = document.querySelectorAll('.connection-line');
        const connections = [];
        
        // Reconstruct connections based on IDs and positions
        nodes.forEach(sourceNode => {
            nodes.forEach(targetNode => {
                const sourceOutput = document.querySelector(`.workflow-node[data-node-id="${sourceNode.id}"] .output-port`);
                const targetInput = document.querySelector(`.workflow-node[data-node-id="${targetNode.id}"] .input-port`);
                
                if (sourceOutput && targetInput) {
                    const connection = isConnected(sourceNode.id, targetNode.id);
                    if (connection) {
                        connections.push({
                            sourceId: sourceNode.id,
                            targetId: targetNode.id
                        });
                    }
                }
            });
        });
        
        // Determine the execution order (topological sort)
        const orderedNodes = [];
        const visited = new Set();
        
        // Start with nodes that have no incoming connections
        const startNodes = nodes.filter(node => 
            !connections.some(conn => conn.targetId === node.id)
        );
        
        // Recursive function to traverse the graph
        function visit(node) {
            if (visited.has(node.id)) return;
            visited.add(node.id);
            
            // Get all outgoing connections
            const outgoing = connections.filter(conn => conn.sourceId === node.id);
            
            // Visit all target nodes
            outgoing.forEach(conn => {
                const targetNode = nodes.find(n => n.id === conn.targetId);
                if (targetNode) visit(targetNode);
            });
            
            // Add this node to the result
            orderedNodes.push(node);
        }
        
        // Visit all start nodes
        startNodes.forEach(node => visit(node));
        
        // Add any remaining nodes (in case of cycles or disconnected nodes)
        nodes.forEach(node => {
            if (!visited.has(node.id)) {
                orderedNodes.push(node);
            }
        });
        
        // Return in reverse order (since we added in post-order)
        return orderedNodes.reverse();
    }
    
    /**
     * Check if two nodes are connected
     */
    function isConnected(sourceId, targetId) {
        // Look for SVG connection lines by class name and position
        const sourceOutput = document.querySelector(`.workflow-node[data-node-id="${sourceId}"] .output-port`);
        const targetInput = document.querySelector(`.workflow-node[data-node-id="${targetId}"] .input-port`);
        
        if (!sourceOutput || !targetInput) return false;
        
        // Simple heuristic: check if any connection visually links these ports
        const connections = document.querySelectorAll('.connection-line');
        
        // For demo purposes, assume a connection exists if there's a line
        // In a real implementation, this would use the connections data structure
        for (const connection of connections) {
            const path = connection.querySelector('path');
            if (path) {
                const pathData = path.getAttribute('d');
                // Very basic check to see if this might connect our nodes
                const sourceRect = sourceOutput.getBoundingClientRect();
                const targetRect = targetInput.getBoundingClientRect();
                
                if (pathData && 
                    pathData.includes(`${Math.round(sourceRect.left)},${Math.round(sourceRect.top)}`) &&
                    pathData.includes(`${Math.round(targetRect.left)},${Math.round(targetRect.top)}`)) {
                    return true;
                }
            }
        }
        
        // For demo purposes, also check any default connections
        if ((sourceId === 'node-1' && targetId === 'node-2') ||
            (sourceId === 'node-2' && targetId === 'node-3')) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Highlight a node during execution
     */
    function highlightNode(nodeId) {
        const nodeElement = document.querySelector(`.workflow-node[data-node-id="${nodeId}"]`);
        if (nodeElement) {
            nodeElement.style.boxShadow = '0 0 0 2px #e74c3c, 0 2px 10px rgba(0, 0, 0, 0.3)';
            nodeElement.style.transform = 'scale(1.05)';
            nodeElement.style.transition = 'all 0.3s ease';
        }
    }
    
    /**
     * Remove highlighting from a node
     */
    function unhighlightNode(nodeId) {
        const nodeElement = document.querySelector(`.workflow-node[data-node-id="${nodeId}"]`);
        if (nodeElement) {
            nodeElement.style.boxShadow = '0 2px 5px rgba(0, 0, 0, 0.2)';
            nodeElement.style.transform = 'scale(1)';
        }
    }
    
    /**
     * Format the output text for display
     */
    function formatOutput(text) {
        if (typeof text !== 'string') {
            return String(text);
        }
        
        // If the text already contains HTML, return it as is
        if (text.includes('<') && text.includes('>')) {
            return text;
        }
        
        // Otherwise, escape HTML and preserve line breaks
        return text
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/\n/g, '<br>');
    }
});